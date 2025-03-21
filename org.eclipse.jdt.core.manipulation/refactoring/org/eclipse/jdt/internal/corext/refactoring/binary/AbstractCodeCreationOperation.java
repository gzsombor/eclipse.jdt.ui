/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.binary;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;

import org.eclipse.jdt.internal.ui.util.Progress;

/**
 * Partial implementation of a code creation operation.
 *
 * @since 3.2
 */
public abstract class AbstractCodeCreationOperation implements IWorkspaceRunnable {

	/** The URI where to output the stubs */
	protected final URI fOutputURI;

	/** The list of packages to create stubs for */
	protected final List<IPackageFragment> fPackages;

	/**
	 * Creates a new abstract code creation operation.
	 *
	 * @param uri
	 *            the URI where to output the code
	 * @param packages
	 *            the list of packages to create code for
	 */
	protected AbstractCodeCreationOperation(final URI uri, final List<IPackageFragment> packages) {
		Assert.isNotNull(uri);
		Assert.isNotNull(packages);
		fOutputURI= uri;
		fPackages= packages;
	}

	/**
	 * Creates a new compilation unit with the given contents.
	 *
	 * @param store
	 *            the file store
	 * @param name
	 *            the name of the compilation unit
	 * @param content
	 *            the content of the compilation unit
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs while creating the compilation unit
	 */
	protected void createCompilationUnit(final IFileStore store, final String name, final String content, final IProgressMonitor monitor) throws CoreException {
		OutputStream stream= null;
		try {
			stream= new BufferedOutputStream(store.getChild(name).openOutputStream(EFS.NONE, Progress.subMonitor(monitor, 1)));
			try {
				stream.write(content.getBytes());
			} catch (IOException exception) {
				throw new CoreException(new Status(IStatus.ERROR, JavaManipulationPlugin.getPluginId(), 0, exception.getLocalizedMessage(), exception));
			}
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException exception) {
					// Do nothing
				}
			}
		}
	}

	/**
	 * Creates a package fragment with the given name.
	 *
	 * @param store
	 *            the file store
	 * @param name
	 *            the name of the package
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs while creating the package fragment
	 */
	protected void createPackageFragment(final IFileStore store, final String name, final IProgressMonitor monitor) throws CoreException {
		store.mkdir(EFS.NONE, monitor);
	}

	/**
	 * Returns the operation label.
	 *
	 * @return the operation label
	 */
	protected abstract String getOperationLabel();

	/**
	 * Runs the stub generation on the specified class file.
	 *
	 * @param file
	 *            the class file
	 * @param parent
	 *            the parent store
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs
	 */
	protected abstract void run(IClassFile file, IFileStore parent, IProgressMonitor monitor) throws CoreException;

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		monitor.beginTask(getOperationLabel(), 100 * fPackages.size());
		try {
			final StringBuilder builder= new StringBuilder(128);
			for (IPackageFragment fragment : fPackages) {
				final IProgressMonitor subMonitor= Progress.subMonitor(monitor, 100);
				final IClassFile[] files= fragment.getOrdinaryClassFiles(); // safe, but implies this operation cannot create module-info CU, which it probably should.
				final int size= files.length;
				subMonitor.beginTask(getOperationLabel(), size * 50);
				final String name= fragment.getElementName();
				IFileStore store= EFS.getStore(fOutputURI);
				if (!"".equals(name)) { //$NON-NLS-1$
					final String pack= name;
					builder.setLength(0);
					builder.append(name);
					final int length= builder.length();
					for (int index= 0; index < length; index++) {
						if (builder.charAt(index) == '.')
							builder.setCharAt(index, '/');
					}
					store= store.getFileStore(new Path(builder.toString()));
					if (!pack.startsWith(".")) //$NON-NLS-1$
						createPackageFragment(store, pack, Progress.subMonitor(subMonitor, 10));
				} else
					createPackageFragment(store, "", Progress.subMonitor(subMonitor, 10)); //$NON-NLS-1$
				final IProgressMonitor subsubMonitor= Progress.subMonitor(subMonitor, 30);
				try {
					subsubMonitor.beginTask(getOperationLabel(), size * 100);
					for (IClassFile file : files) {
						if (subMonitor.isCanceled())
							throw new OperationCanceledException();
						run(file, store, Progress.subMonitor(subsubMonitor, 100));
					}
				} finally {
					subsubMonitor.done();
				}
			}
		} finally {
			monitor.done();
		}
	}
}
