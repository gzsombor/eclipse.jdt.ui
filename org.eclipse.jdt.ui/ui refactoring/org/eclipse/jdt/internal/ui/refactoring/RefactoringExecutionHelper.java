/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.IUndoManager;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.refactoring.changes.ChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class RefactoringExecutionHelper {

	private IRefactoring fRefactoring;
	private Shell fParent;
	private IRunnableContext fExecContext;
	private int fStopSeverity;
	private ChangeContext fContext;

	private class Operation implements IRunnableWithProgress {
		public IChange fChange;
		public void run(IProgressMonitor pm) throws InvocationTargetException, InterruptedException {
			try {
				pm.beginTask("", 10); //$NON-NLS-1$
				pm.subTask(""); //$NON-NLS-1$
				RefactoringStatus status= fRefactoring.checkPreconditions(new SubProgressMonitor(pm, 4, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
				if (status.getSeverity() >= fStopSeverity) {
					RefactoringStatusDialog dialog= new RefactoringStatusDialog(fParent, status, fRefactoring.getName());
					if(dialog.open() == IDialogConstants.CANCEL_ID) {
						throw new InterruptedException();
					}
				}
				fChange= fRefactoring.createChange(new SubProgressMonitor(pm, 2, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
				boolean success= false;
				IUndoManager undoManager= Refactoring.getUndoManager();
				try {
					fChange.aboutToPerform(fContext, new NullProgressMonitor());
					JavaCore.run(new IWorkspaceRunnable() {
						public void run(IProgressMonitor monitor) throws CoreException {
							fChange.perform(fContext, monitor);
						}
					}, new SubProgressMonitor(pm, 4, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
				} finally {
					fChange.performed();
				}
			} catch (ChangeAbortException e) {
				throw new InvocationTargetException(e);
		    } catch (CoreException e) {
				throw new InvocationTargetException(e);
			} finally {
				pm.done();
			}
		}
		public boolean isExecuted() {
			return fChange != null;
		}
		public boolean isUndoable() {
			return fChange.isUndoable();
		}
		public IChange getUndoChange() {
			return fChange.getUndoChange();
		}
	}
	
	public RefactoringExecutionHelper(IRefactoring refactoring, int stopSevertity, Shell parent, IRunnableContext context) {
		super();
		Assert.isNotNull(refactoring);
		Assert.isNotNull(parent);
		Assert.isNotNull(context);
		fRefactoring= refactoring;
		fStopSeverity= stopSevertity;
		fParent= parent;
		fExecContext= context;
	}
	
	public void perform() throws InterruptedException, InvocationTargetException {
		RefactoringSaveHelper saveHelper= new RefactoringSaveHelper();
		if (!saveHelper.saveEditors())
			throw new InterruptedException();
		fContext= new ChangeContext(new ChangeExceptionHandler(fParent));
		boolean success= false;
		IUndoManager undoManager= Refactoring.getUndoManager();
		Operation op= new Operation();
		try{
			undoManager.aboutToPerformRefactoring();
			fExecContext.run(false, false, op);
			if (op.isExecuted()) {
				if (!op.isUndoable()) {
					success= false;
				} else { 
					undoManager.addUndo(fRefactoring.getName(), op.getUndoChange());
					success= true;
				}
			} 
		} catch (InvocationTargetException e) {
			Throwable t= e.getTargetException();
			if (t instanceof ChangeAbortException) {
				handleChangeAbortException((ChangeAbortException)t);
			} else {
				throw e;
			}
		} finally {
			fContext.clearPerformedChanges();
			undoManager.refactoringPerformed(success);
			saveHelper.triggerBuild();
		}
	}
	
	private void handleChangeAbortException(ChangeAbortException exception) {
		if (!fContext.getTryToUndo())
			return;
			
		IRunnableWithProgress op= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InterruptedException, InvocationTargetException {
				try {
					JavaCore.run(new IWorkspaceRunnable() {
						public void run(IProgressMonitor pm) throws CoreException {
							ChangeContext undoContext= new ChangeContext(new AbortChangeExceptionHandler());
							IChange[] changes= fContext.getPerformedChanges();
							pm.beginTask(RefactoringMessages.getString("RefactoringWizard.undoing"), changes.length); //$NON-NLS-1$
							IProgressMonitor sub= new NullProgressMonitor();
							for (int i= changes.length - 1; i >= 0; i--) {
								IChange change= changes[i];
								pm.subTask(change.getName());
								change.getUndoChange().perform(undoContext, sub);
								pm.worked(1);
							}
						}
					}, pm);
				} catch (ChangeAbortException e) {
					throw new InvocationTargetException(e.getThrowable());
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					pm.done();
				} 
			}
		};
		
		try {
			fExecContext.run(false, false, op);
		} catch (InvocationTargetException e) {
			handleUnexpectedException(e);
		} catch (InterruptedException e) {
			// not possible. Operation not cancelable.
		}
	}
	
	private void handleUnexpectedException(InvocationTargetException e) {
		ExceptionHandler.handle(e, RefactoringMessages.getString("RefactoringWizard.refactoring"), RefactoringMessages.getString("RefactoringWizard.unexpected_exception_1")); //$NON-NLS-2$ //$NON-NLS-1$
	}
}
