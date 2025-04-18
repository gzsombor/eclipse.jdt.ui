/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.dialogs.NewFolderDialog;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ExclusionInclusionDialog;

/**
 * Helper class for queries used by the <code>ClasspathModifier</code>.
 * Clients can either decide to implement their own queries or just taking
 * the predefined queries.
 */
public class ClasspathModifierQueries {

	/**
     * A validator for the output location that can be
     * used to find out whether the entred location can be
     * used for an output folder or not.
     */
    public static abstract class OutputFolderValidator {
        protected IClasspathEntry[] fEntries;
        protected List<?> fElements;

        /**
         * Create a output folder validator.
         *
         * @param newElements a list of elements that will be added
         * to the buildpath. The list's items can be of type:
         * <ul>
         * <li><code>IJavaProject</code></li>
         * <li><code>IPackageFragment</code></li>
         * <li><code>IFolder</code></li>
         * </ul>
         * @param project the Java project
         */
        public OutputFolderValidator(List<?> newElements, IJavaProject project) throws JavaModelException {
            fEntries= project.getRawClasspath();
            fElements= newElements;
        }

        /**
         * The path of the output location to be validated. The path
         * should contain the full path within the project, for example:
         * /ProjectXY/folderA/outputLocation.
         *
         * @param outputLocation the output location for the project
         * @return <code>true</code> if the output location is valid,
         * <code>false</code> otherwise.
         */
        public abstract boolean validate(IPath outputLocation);
    }

    /**
     * Query to get information about whether the project should be removed as
     * source folder and update build folder to <code>outputLocation</code>
     */
    public static abstract class OutputFolderQuery {
        protected IPath fDesiredOutputLocation;

        /**
         * Constructor gets the desired output location
         * of the project
         *
         * @param outputLocation desired output location for the
         * project. It is possible that the desired output location
         * equals the current project's output location (for example if
         * it is not intended to change the output location at this time).
         */
        public OutputFolderQuery(IPath outputLocation) {
            if (outputLocation != null)
                fDesiredOutputLocation= outputLocation.makeAbsolute();
        }

        /**
         * Getter for the desired output location.
         *
         * @return the project's desired output location
         */
        public IPath getDesiredOutputLocation() {
            return fDesiredOutputLocation;
        }

        /**
         * Get the output location that was determined by the
         * query for the project. Note that this output location
         * does not have to be the same as the desired output location
         * that is passed to the constructor.
         *
         * This method is only intended to be called if <code>doQuery</code>
         * has been executed successfully and had return <code>true</code> to
         * indicate that changes were accepted.
         *
         *@return the effective output location
         */
        public abstract IPath getOutputLocation();

        /**
         * Find out wheter the project should be removed from the classpath
         * or not.
         *
         * This method is only intended to be called if <code>doQuery</code>
         * has been executed successfully and had return <code>true</code> to
         * indicate that changes were accepted.
         *
         * @return <code>true</code> if the project should be removed from
         * the classpath, <code>false</code> otherwise.
         */
        public abstract boolean removeProjectFromClasspath();

        /**
         * Query to get information about whether the project should be removed as
         * source folder and update build folder to <code>outputLocation</code>.
         *
         * There are several situations for setting up a project where it is not possible
         * to have the project folder itself as output folder. Therefore, the query asks in the
         * first place for changing the output folder. Additionally, it also can be usefull to
         * remove the project from the classpath. This information can be retrieved by calling
         * <code>removeProjectFromClasspath()</code>.
         *
         * Note: if <code>doQuery</code> returns false, the started computation will stop immediately.
         * There is no additional dialog that informs the user about this abort. Therefore it is important
         * that the query informs the users about the consequences of not allowing to change the output
         * folder.
         *
         * @param editingOutputFolder <code>true</code> if currently an output folder is changed,
         * <code>false</code> otherwise. This information can be usefull to generate an appropriate
         * message to ask the user for an action.
         * @param validator a validator to find out whether the chosen output location is valid or not
         * @param project the Java project
         * @return <code>true</code> if the execution was successfull (e.g. not aborted) and
         * the caller should execute additional steps as setting the output location for the project or (optionally)
         * removing the project from the classpath, <code>false</code> otherwise.
         * @throws JavaModelException if the output location of the project could not be retrieved
         */
        public abstract boolean doQuery(final boolean editingOutputFolder, final OutputFolderValidator validator, final IJavaProject project) throws JavaModelException;

    }

    /**
     * Query to get information about the inclusion and exclusion filters of
     * an element.
     */
    public interface IInclusionExclusionQuery {
        /**
         * Query to get information about the
         * inclusion and exclusion filters of
         * an element.
         *
         * While executing <code>doQuery</code>,
         * these filter might change.
         *
         * On calling <code>getInclusionPattern()</code>
         * or <code>getExclusionPattern()</code> it
         * is expected to get the new and updated
         * filters back.
         *
         * @param element the element to get the
         * information from
         * @return <code>true</code> if changes
         * have been accepted and <code>getInclusionPatter</code>
         * or <code>getExclusionPattern</code> can
         * be called.
         */
        boolean doQuery(CPListElement element, boolean focusOnExcluded);

        /**
         * Can only be called after <code>
         * doQuery</code> has been executed and
         * has returned <code>true</code>
         *
         * @return the new inclusion filters
         */
        IPath[] getInclusionPattern();

        /**
         * Can only be called after <code>
         * doQuery</code> has been executed and
         * has returned <code>true</code>
         *
         * @return the new exclusion filters
         */
        IPath[] getExclusionPattern();
    }

    /**
     * Query to get information about the output location that should be used for a
     * given element.
     */
    public interface IOutputLocationQuery {
        /**
         * Query to get information about the output
         * location that should be used for a
         * given element.
         *
         * @param element the element to get
         * an output location for
         * @return <code>true</code> if the output
         * location has changed, <code>false</code>
         * otherwise.
         */
        boolean doQuery(CPListElement element);

        /**
         * Gets the new output location.
         *
         * May only be called after having
         * executed <code>doQuery</code> which
         * must have returned <code>true</code>
         *
         * @return the new output location, can be <code>null</code>
         */
        IPath getOutputLocation();

        /**
         * Get a query for information about whether the project should be removed as
         * source folder and update build folder
         *
         * @param outputLocation desired output location for the
         * project
         * @return query giving information about output and source folders
         */
        OutputFolderQuery getOutputFolderQuery(IPath outputLocation) throws JavaModelException;
    }

    /**
	 * Query to determine whether a linked folder should be removed.
	 */
	public interface IRemoveLinkedFolderQuery {

		/** Remove status indicating that the removal should be cancelled */
		int REMOVE_CANCEL= 0;

		/** Remove status indicating that the folder should be removed from the build path only */
		int REMOVE_BUILD_PATH= 1;

		/** Remove status indicating that the folder should be removed from the build path and deleted */
		int REMOVE_BUILD_PATH_AND_FOLDER= 2;

		/**
		 * Query to determined whether the linked folder should be removed as well.
		 *
		 * @param folder the linked folder to remove
		 * @return a status code corresponding to one of the IRemoveLinkedFolderQuery#REMOVE_XXX constants
		 */
		int doQuery(IFolder folder);
	}

    /**
	 * Query to create a folder.
	 */
    public interface ICreateFolderQuery {
        /**
         * Query to create a folder.
         *
         * @return <code>true</code> if the operation
         * was successful (e.g. no cancelled), <code>
         * false</code> otherwise
         */
        boolean doQuery();

        /**
         * Find out whether a source folder is about
         * to be created or a normal folder which
         * is not on the classpath (and therefore
         * might have to be excluded).
         *
         * Should only be called after having executed
         * <code>doQuery</code>, because otherwise
         * it might not be sure if a result exists or
         * not.
         *
         * @return <code>true</code> if a source
         * folder should be created, <code>false
         * </code> otherwise
         */
        boolean isSourceFolder();

        /**
         * Get the newly created folder.
         * This method is only valid after having
         * called <code>doQuery</code>.
         *
         * @return the created folder of type
         * <code>IFolder</code>
         */
        IFolder getCreatedFolder();
    }

    /**
     * Query to add archives (.jar or .zip files) to the buildpath.
     */
    public interface IAddArchivesQuery {
        /**
         * Get the paths to the new archive entries that should be added to the buildpath.
         *
         * @return Returns the new classpath container entry paths or an empty array if the query has
         * been cancelled by the user.
         */
        IPath[] doQuery();
    }

    /**
     * Query to add libraries to the buildpath.
     */
    public interface IAddLibrariesQuery {
        /**
         * Get the new classpath entries for libraries to be added to the buildpath.
         *
         * @param project the Java project
         * @param entries an array of classpath entries for the project
         * @return Returns the selected classpath container entries or an empty if the query has
         * been cancelled by the user.
         */
        IClasspathEntry[] doQuery(final IJavaProject project, final IClasspathEntry[] entries);
    }

    /**
     * The query is used to get information about whether the project should be removed as
     * source folder and update build folder to <code>outputLocation</code>
     *
     * @param shell shell if there is any or <code>null</code>
     * @param outputLocation the desired project's output location
     * @return an <code>IOutputFolderQuery</code> that can be executed
     *
     * @see OutputFolderQuery
     */
    public static OutputFolderQuery getDefaultFolderQuery(final Shell shell, IPath outputLocation) {

        return new OutputFolderQuery(outputLocation) {
			protected IPath fOutputLocation;
			protected boolean fRemoveProject;

            @Override
			public boolean doQuery(final boolean editingOutputFolder,  final OutputFolderValidator validator, final IJavaProject project) throws JavaModelException {
                fRemoveProject= false;
                fOutputLocation= project.getOutputLocation();
				return Display.getDefault().syncCall(() -> {
					Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();

					String title= NewWizardMessages.ClasspathModifier_ChangeOutputLocationDialog_title;

					if (fDesiredOutputLocation.segmentCount() == 1) {
						String outputFolderName= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME);
						IPath newOutputFolder1= fDesiredOutputLocation.append(outputFolderName);
						newOutputFolder1= getValidPath(newOutputFolder1, validator);
						String message1= Messages.format(NewWizardMessages.ClasspathModifier_ChangeOutputLocationDialog_project_outputLocation, BasicElementLabels.getPathLabel(newOutputFolder1, false));
						fRemoveProject= true;
						if (MessageDialog.openConfirm(sh, title, message1)) {
							fOutputLocation= newOutputFolder1;
							return true;
						}
					} else {
						IPath newOutputFolder2= fDesiredOutputLocation;
						newOutputFolder2= getValidPath(newOutputFolder2, validator);
						if (editingOutputFolder) {
							fOutputLocation= newOutputFolder2;
							return true; // show no dialog
						}
						String message2= NewWizardMessages.ClasspathModifier_ChangeOutputLocationDialog_project_message;
						fRemoveProject= true;
						if (MessageDialog.openQuestion(sh, title, message2)) {
							fOutputLocation= newOutputFolder2;
							return true;
						}
					}
					return false;
				});
            }

            @Override
			public IPath getOutputLocation() {
                return fOutputLocation;
            }

            @Override
			public boolean removeProjectFromClasspath() {
                return fRemoveProject;
            }

            private IPath getValidPath(IPath newOutputFolder, OutputFolderValidator validator) {
                int i= 1;
                IPath path= newOutputFolder;
                while (!validator.validate(path)) {
                    path= new Path(newOutputFolder.toString() + i);
                    i++;
                }
                return path;
            }
        };
    }

    /**
     * A default query for inclusion and exclusion filters.
     * The query is used to get information about the
     * inclusion and exclusion filters of an element.
     *
     * @param shell shell if there is any or <code>null</code>
     * @return an <code>IInclusionExclusionQuery</code> that can be executed
     *
     * @see ClasspathModifierQueries.IInclusionExclusionQuery
     */
	public static IInclusionExclusionQuery getDefaultInclusionExclusionQuery(final Shell shell) {
		return new IInclusionExclusionQuery() {

			protected IPath[] fInclusionPattern;
			protected IPath[] fExclusionPattern;

			@Override
			public boolean doQuery(final CPListElement element, final boolean focusOnExcluded) {
				return Display.getDefault().syncCall(() -> {
					Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();
					ExclusionInclusionDialog dialog= new ExclusionInclusionDialog(sh, element, focusOnExcluded);
					boolean result = dialog.open() == Window.OK;
					fInclusionPattern= dialog.getInclusionPattern();
					fExclusionPattern= dialog.getExclusionPattern();
					return result;
				});
			}

			@Override
			public IPath[] getInclusionPattern() {
				return fInclusionPattern;
			}

			@Override
			public IPath[] getExclusionPattern() {
				return fExclusionPattern;
			}
		};
	}

    /**
     * Shows the UI to select new external JAR or ZIP archive entries. If the query
     * was aborted, the result is an empty array.
     *
     * @param shell The parent shell for the dialog, can be <code>null</code>
     * @return an <code>IAddArchivesQuery</code> showing a dialog to selected archive files
     * to be added to the buildpath
     *
     * @see IAddArchivesQuery
     */
    public static IAddArchivesQuery getDefaultArchivesQuery(final Shell shell) {
        return () -> {
		    final IPath[] selected = Display.getDefault().syncCall(() -> {
			    Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();
			    return BuildPathDialogAccess.chooseExternalJAREntries(sh);
			});
		    if(selected == null)
		        return new IPath[0];
		    return selected;
		};
    }

    /**
	 * Shows the UI to prompt whether a linked folder which has been removed from the build path should be deleted as well.
	 *
	 * @param shell The parent shell for the dialog, can be <code>null</code>
	 * @return an <code>IRemoveLinkedFolderQuery</code> showing a dialog to prompt whether the linked folder should be deleted as well
	 *
	 * @see IRemoveLinkedFolderQuery
	 */
	public static IRemoveLinkedFolderQuery getDefaultRemoveLinkedFolderQuery(final Shell shell) {
		return folder -> {
			return Display.getDefault().syncCall(() -> {
				final RemoveLinkedFolderDialog dialog= new RemoveLinkedFolderDialog((shell != null ? shell : JavaPlugin.getActiveWorkbenchShell()), folder);
				final int status= dialog.open();
				if (status == 0) {
					return dialog.getRemoveStatus();
				} else {
					return IRemoveLinkedFolderQuery.REMOVE_CANCEL;
				}
			});
		};
	}

    /**
     * Shows the UI to choose new classpath container classpath entries. See {@link IClasspathEntry#CPE_CONTAINER} for
     * details about container classpath entries.
     * The query returns the selected classpath entries or an empty array if the query has
     * been cancelled.
     *
     * @param shell The parent shell for the dialog, can be <code>null</code>
     * @return Returns the selected classpath container entries or an empty array if the query has
     * been cancelled by the user.
     */
    public static IAddLibrariesQuery getDefaultLibrariesQuery(final Shell shell) {
        return (project, entries) -> {
		    final IClasspathEntry[] selected= Display.getDefault().syncCall(() -> {
			    Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();
			    return BuildPathDialogAccess.chooseContainerEntries(sh, project, entries);
			});
		    if(selected == null) {
		        return new IClasspathEntry[0];
		    }
		    return selected;
		};
    }

    /**
     * Shows the UI to create a new source folder.
     *
     * @param shell The parent shell for the dialog, can be <code>null</code>
     * @param project the Java project to create the source folder for
     * @return returns the query
     */
	public static ICreateFolderQuery getDefaultCreateFolderQuery(final Shell shell, final IJavaProject project) {
		return new ICreateFolderQuery() {

			private IFolder fNewFolder;

			@Override
			public boolean doQuery() {
				return Display.getDefault().syncCall(() -> {
				    Shell sh= shell != null ? shell : JavaPlugin.getActiveWorkbenchShell();

				    NewFolderDialog dialog= new NewFolderDialog(sh, project.getProject());
				    if (dialog.open() == Window.OK) {
				    	IResource sourceContainer= (IResource) dialog.getResult()[0];
				    	if (sourceContainer instanceof IFolder) {
				    		fNewFolder= (IFolder)sourceContainer;
				    	} else {
				    		fNewFolder= null;
				    	}
				    	return true;
				    } else {
				    	return false;
				    }
				});
			}


			@Override
			public boolean isSourceFolder() {
				return true;
			}

			@Override
			public IFolder getCreatedFolder() {
				return fNewFolder;
			}

		};
	}

	private ClasspathModifierQueries() {
	}
}
