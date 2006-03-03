/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.AbstractJavaElementRenameChange;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.Messages;

public class RenameSourceFolderChange extends AbstractJavaElementRenameChange {

	public static final String ID_RENAME_SOURCE_FOLDER= "org.eclipse.jdt.ui.rename.source.folder"; //$NON-NLS-1$

	public RenameSourceFolderChange(IPackageFragmentRoot sourceFolder, String newName, String comment) {
		this(sourceFolder.getPath(), sourceFolder.getElementName(), newName, comment, IResource.NULL_STAMP);
		Assert.isTrue(!sourceFolder.isReadOnly(), "should not be read only");  //$NON-NLS-1$
		Assert.isTrue(!sourceFolder.isArchive(), "should not be an archive");  //$NON-NLS-1$
	}
	
	private RenameSourceFolderChange(IPath resourcePath, String oldName, String newName, String comment, long stampToRestore) {
		super(resourcePath, oldName, newName, comment);
	}
	
	protected IPath createNewPath(){
		return getResourcePath().removeLastSegments(1).append(getNewName());
	}
	
	protected Change createUndoChange(long stampToRestore) {
		return new RenameSourceFolderChange(createNewPath(), getNewName(), getOldName(), getComment(), stampToRestore);
	}

	public String getName() {
		return Messages.format(RefactoringCoreMessages.RenameSourceFolderChange_rename, 
			new String[]{getOldName(), getNewName()});
	}

	protected void doRename(IProgressMonitor pm) throws CoreException {
		IPackageFragmentRoot sourceFolder= getSourceFolder();
		if (sourceFolder != null)
			sourceFolder.move(getNewPath(), getCoreMoveFlags(), getJavaModelUpdateFlags(), null, pm);
	}

	private IPath getNewPath() {
		return getResource().getFullPath().removeLastSegments(1).append(getNewName());
	}

	private IPackageFragmentRoot getSourceFolder() {
		return (IPackageFragmentRoot)getModifiedElement();
	}

	private int getJavaModelUpdateFlags() {
		return 	IPackageFragmentRoot.DESTINATION_PROJECT_CLASSPATH 
				| 	IPackageFragmentRoot.ORIGINATING_PROJECT_CLASSPATH
				| 	IPackageFragmentRoot.OTHER_REFERRING_PROJECTS_CLASSPATH
				| 	IPackageFragmentRoot.REPLACE;
	}

	private int getCoreMoveFlags() {
		if (getResource().isLinked())
			return IResource.SHALLOW;
		else
			return IResource.NONE;
	}
	
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 2); //$NON-NLS-1$
		result.merge(isValid(new SubProgressMonitor(pm, 1), DIRTY));
		if (result.hasFatalError())
			return result;
		IPackageFragmentRoot sourceFolder= getSourceFolder();
		result.merge(checkIfModifiable(sourceFolder, new SubProgressMonitor(pm, 1)));
		
		return result;
	}
	
	private static RefactoringStatus checkIfModifiable(IPackageFragmentRoot root, IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		checkExistence(result, root);
		if (result.hasFatalError())
			return result;
		
		if (root.isArchive()) {
			result.addFatalError(Messages.format(RefactoringCoreMessages.RenameSourceFolderChange_rename_archive, root.getElementName()));
			return result;
		}
		
		if (root.isExternal()) {
			result.addFatalError(Messages.format(RefactoringCoreMessages.RenameSourceFolderChange_rename_external, root.getElementName()));
			return result;
		}
		
		checkExistence(result, root.getCorrespondingResource());
		if (result.hasFatalError())
			return result;
		
		if (root.getCorrespondingResource().isLinked()) {
			result.addFatalError(Messages.format(RefactoringCoreMessages.RenameSourceFolderChange_rename_linked, root.getElementName()));
			return result;
		}
				
		return result;
	}

	public final ChangeDescriptor getDescriptor() {
		String project= null;
		final IProject container= getResource().getProject();
		if (container != null)
			project= container.getName();
		final Map arguments= new HashMap();
		final JavaRefactoringDescriptor descriptor= new JavaRefactoringDescriptor(ID_RENAME_SOURCE_FOLDER, project, Messages.format(RefactoringCoreMessages.RenameSourceFolderChange_descriptor_description, new String[] { getResourcePath().toString(), getNewName()}), getComment(), arguments, RefactoringDescriptor.NONE);
		arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(getSourceFolder()));
		arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_NAME, getNewName());
		return new RefactoringChangeDescriptor(descriptor);
	}
}