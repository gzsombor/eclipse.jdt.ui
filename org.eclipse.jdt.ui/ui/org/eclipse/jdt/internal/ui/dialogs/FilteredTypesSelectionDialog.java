/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.dialogs;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.BoldStylerProvider;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import org.eclipse.jdt.core.search.TypeNameRequestor;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.CollectionsUtil;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.OpenTypeHistory;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.TypeFilter;
import org.eclipse.jdt.internal.corext.util.TypeInfoFilter;
import org.eclipse.jdt.internal.corext.util.TypeInfoRequestorAdapter;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.ProblemsLabelDecorator;
import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.jdt.ui.dialogs.ITypeInfoImageProvider;
import org.eclipse.jdt.ui.dialogs.ITypeSelectionComponent;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.preferences.TypeFilterPreferencePage;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.TypeNameMatchLabelProvider;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetFilterActionGroup;


/**
 * Shows a list of Java types to the user with a text entry field for a string
 * pattern used to filter the list of types.
 *
 * @since 3.3
 */
public class FilteredTypesSelectionDialog extends FilteredItemsSelectionDialog implements ITypeSelectionComponent {

	private static final String DIALOG_SETTINGS= "org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog"; //$NON-NLS-1$

	private static final String WORKINGS_SET_SETTINGS= "WorkingSet"; //$NON-NLS-1$

	private WorkingSetFilterActionGroup fFilterActionGroup;

	private final TypeItemLabelProvider fTypeInfoLabelProvider;

	private String fTitle;

	private IJavaSearchScope fSearchScope;

	private boolean fAllowScopeSwitching;

	/**
	 * Flags defining nature of searched elements; the only valid
	 * values are:
	 *  {@link IJavaSearchConstants#TYPE},
	 * 	{@link IJavaSearchConstants#ANNOTATION_TYPE},
	 * 	{@link IJavaSearchConstants#INTERFACE},
	 * 	{@link IJavaSearchConstants#ENUM},
	 * 	{@link IJavaSearchConstants#CLASS_AND_INTERFACE},
	 * 	{@link IJavaSearchConstants#CLASS_AND_ENUM}.
	 */
	private final int fElementKinds;

	private final ITypeInfoFilterExtension fFilterExtension;

	private final TypeSelectionExtension fExtension;

	private ISelectionStatusValidator fValidator;

	private final TypeInfoUtil fTypeInfoUtil;

	private static boolean fgFirstTime= true;

	private final TypeItemsComparator fTypeItemsComparator;

	private int fTypeFilterVersion= 0;

	private TypeItemsFilter fFilter;

	/**
	 * Creates new FilteredTypesSelectionDialog instance
	 *
	 * @param parent
	 *            shell to parent the dialog on
	 * @param multi
	 *            <code>true</code> if multiple selection is allowed
	 * @param context
	 *            context used to execute long-running operations associated
	 *            with this dialog
	 * @param scope
	 *            scope used when searching for types
	 * @param elementKinds
	 *            flags defining nature of searched elements; the only valid
	 *            values are: <code>IJavaSearchConstants.TYPE</code>
	 * 	<code>IJavaSearchConstants.ANNOTATION_TYPE</code>
	 * 	<code>IJavaSearchConstants.INTERFACE</code>
	 * 	<code>IJavaSearchConstants.ENUM</code>
	 * 	<code>IJavaSearchConstants.CLASS_AND_INTERFACE</code>
	 * 	<code>IJavaSearchConstants.CLASS_AND_ENUM</code>.
	 *            Please note that the bitwise OR combination of the elementary
	 *            constants is not supported.
	 */
	public FilteredTypesSelectionDialog(Shell parent, boolean multi, IRunnableContext context, IJavaSearchScope scope, int elementKinds) {
		this(parent, multi, context, scope, elementKinds, null);
	}

	/**
	 * Creates new FilteredTypesSelectionDialog instance.
	 *
	 * @param shell
	 *            shell to parent the dialog on
	 * @param multi
	 *            <code>true</code> if multiple selection is allowed
	 * @param context
	 *            context used to execute long-running operations associated
	 *            with this dialog
	 * @param scope
	 *            scope used when searching for types. If the scope is <code>null</code>,
	 *            then workspace is scope is used as default, and the user can
	 *            choose a working set as scope.
	 * @param elementKinds
	 *            flags defining nature of searched elements; the only valid
	 *            values are: <code>IJavaSearchConstants.TYPE</code>
	 * 	<code>IJavaSearchConstants.ANNOTATION_TYPE</code>
	 * 	<code>IJavaSearchConstants.INTERFACE</code>
	 * 	<code>IJavaSearchConstants.ENUM</code>
	 * 	<code>IJavaSearchConstants.CLASS_AND_INTERFACE</code>
	 * 	<code>IJavaSearchConstants.CLASS_AND_ENUM</code>.
	 *            Please note that the bitwise OR combination of the elementary
	 *            constants is not supported.
	 * @param extension
	 *            an extension of the standard type selection dialog; See
	 *            {@link TypeSelectionExtension}
	 */
	public FilteredTypesSelectionDialog(Shell shell, boolean multi, IRunnableContext context, IJavaSearchScope scope, int elementKinds, TypeSelectionExtension extension) {
		super(shell, multi);

		setSelectionHistory(new TypeSelectionHistory());

		if (scope == null) {
			fAllowScopeSwitching= true;
			scope= SearchEngine.createWorkspaceScope();
		}

		fElementKinds= elementKinds;
		fExtension= extension;
		fFilterExtension= (extension == null) ? null : extension.getFilterExtension();
		fSearchScope= scope;

		if (extension != null) {
			fValidator= extension.getSelectionValidator();
		}

		fTypeInfoUtil= new TypeInfoUtil(extension != null ? extension.getImageProvider() : null);

		fTypeInfoLabelProvider= new TypeItemLabelProvider(new ProblemsLabelDecorator(null));

		setListLabelProvider(fTypeInfoLabelProvider);
		setListSelectionLabelDecorator(fTypeInfoLabelProvider);

		setDetailsLabelProvider(new TypeItemDetailsLabelProvider(fTypeInfoUtil));

		fTypeItemsComparator= new TypeItemsComparator();
	}

	@Override
	public void setTitle(String title) {
		super.setTitle(title);
		fTitle= title;
	}

	/**
	 * Adds or replaces subtitle of the dialog
	 *
	 * @param text
	 *            the new subtitle for this dialog
	 */
	private void setSubtitle(String text) {
		if (text == null || text.length() == 0) {
			getShell().setText(fTitle);
		} else {
			getShell().setText(Messages.format(JavaUIMessages.FilteredTypeSelectionDialog_titleFormat, new String[] { fTitle, text }));
		}
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings= JavaPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}

	@Override
	protected void storeDialog(IDialogSettings settings) {
		super.storeDialog(settings);

		if (fFilterActionGroup != null) {
			XMLMemento memento= XMLMemento.createWriteRoot("workingSet"); //$NON-NLS-1$
			fFilterActionGroup.saveState(memento);
			fFilterActionGroup.dispose();
			StringWriter writer= new StringWriter();
			try {
				memento.save(writer);
				settings.put(WORKINGS_SET_SETTINGS, writer.getBuffer().toString());
			} catch (IOException e) {
				// don't do anything. Simply don't store the settings
				JavaPlugin.log(e);
			}
		}
	}

	@Override
	protected void restoreDialog(IDialogSettings settings) {
		super.restoreDialog(settings);

		if (fAllowScopeSwitching) {
			String setting= settings.get(WORKINGS_SET_SETTINGS);
			if (setting != null) {
				try {
					IMemento memento= XMLMemento.createReadRoot(new StringReader(setting));
					fFilterActionGroup.restoreState(memento);
				} catch (WorkbenchException e) {
					// don't do anything. Simply don't restore the settings
					JavaPlugin.log(e);
				}
			}
			IWorkingSet ws= fFilterActionGroup.getWorkingSet();
			if (ws == null || (ws.isAggregateWorkingSet() && ws.isEmpty())) {
				setSearchScope(SearchEngine.createWorkspaceScope());
				setSubtitle(null);
			} else {
				setSearchScope(JavaSearchScopeFactory.getInstance().createJavaSearchScope(ws, true));
				setSubtitle(ws.getLabel());
			}
		}

		// TypeNameMatch[] types = OpenTypeHistory.getInstance().getTypeInfos();
		//
		// for (int i = 0; i < types.length; i++) {
		// TypeNameMatch type = types[i];
		// accessedHistoryItem(type);
		// }
	}

	@Override
	protected void fillViewMenu(IMenuManager menuManager) {
		super.fillViewMenu(menuManager);

		if (fAllowScopeSwitching) {
			fFilterActionGroup= new WorkingSetFilterActionGroup(getShell(), JavaPlugin.getActivePage(), event -> {
				IWorkingSet ws= (IWorkingSet) event.getNewValue();
				if (ws == null || (ws.isAggregateWorkingSet() && ws.isEmpty())) {
					setSearchScope(SearchEngine.createWorkspaceScope());
					setSubtitle(null);
				} else {
					setSearchScope(JavaSearchScopeFactory.getInstance().createJavaSearchScope(ws, true));
					setSubtitle(ws.getLabel());
				}

				applyFilter();
			});
			fFilterActionGroup.fillViewMenu(menuManager);
		}

		menuManager.add(new Separator());
		menuManager.add(new TypeFiltersPreferencesAction());
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		Control addition= null;

		if (fExtension != null) {

			addition= fExtension.createContentArea(parent);
			if (addition != null) {
				GridData gd= new GridData(GridData.FILL_HORIZONTAL);
				gd.horizontalSpan= 2;
				addition.setLayoutData(gd);

			}

			fExtension.initialize(this);
		}

		return addition;
	}

	@Override
	protected void setResult(List newResult) {

		List<IType> resultToReturn= new ArrayList<>();

		for (Object element : newResult) {
			if (element instanceof TypeNameMatch) {
				IType type= ((TypeNameMatch) element).getType();
				if (type.exists()) {
					// items are added to history in the
					// org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#computeResult()
					// method
					resultToReturn.add(type);
				} else {
					TypeNameMatch typeInfo= (TypeNameMatch) element;
					IPackageFragmentRoot root= typeInfo.getPackageFragmentRoot();
					String containerName= JavaElementLabels.getElementLabel(root, JavaElementLabels.ROOT_QUALIFIED);
					String message= Messages.format(JavaUIMessages.FilteredTypesSelectionDialog_dialogMessage, new String[] { TypeNameMatchLabelProvider.getText(typeInfo, TypeNameMatchLabelProvider.SHOW_FULLYQUALIFIED), containerName });
					MessageDialog.openError(getShell(), fTitle, message);
					getSelectionHistory().remove(typeInfo);
				}
			}
		}

		super.setResult(resultToReturn);
	}

	/*
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#create()
	 */
	@Override
	public void create() {
		super.create();
		Control patternControl= getPatternControl();
		if (patternControl instanceof Text) {
			TextFieldNavigationHandler.install((Text) patternControl);
		}
	}

	@Override
	public int open() {
		if (getInitialPattern() == null) {
			IWorkbenchWindow window= JavaPlugin.getActiveWorkbenchWindow();
			if (window != null) {
				ISelection selection= window.getSelectionService().getSelection();
				if (selection instanceof ITextSelection) {
					String text= ((ITextSelection) selection).getText();
					if (text != null) {
						text= text.trim();
						if (text.length() > 0 && JavaConventions.validateJavaTypeName(text, JavaCore.VERSION_1_8, JavaCore.VERSION_1_8, null).isOK()) {
							setInitialPattern(text, FULL_SELECTION);
						}
					}
				}
			}
		}
		return super.open();
	}

	/**
	 * Sets a new validator.
	 *
	 * @param validator
	 *            the new validator
	 */
	public void setValidator(ISelectionStatusValidator validator) {
		fValidator= validator;
	}

	@Override
	protected ItemsFilter createFilter() {
		fFilter= new TypeItemsFilter(fSearchScope, fElementKinds, fFilterExtension);
		return fFilter;
	}

	/*
	 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IJavaHelpContextIds.TYPE_SELECTION_DIALOG2);
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider provider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
		TypeItemsFilter typeSearchFilter= (TypeItemsFilter) itemsFilter;
		TypeSearchRequestor requestor= new TypeSearchRequestor(provider, typeSearchFilter);
		SearchEngine engine= new SearchEngine((WorkingCopyOwner) null);
		String packPattern= typeSearchFilter.getPackagePattern();
		progressMonitor.setTaskName(JavaUIMessages.FilteredTypesSelectionDialog_searchJob_taskName);

		/*
		 * Setting the filter into match everything mode avoids filtering twice
		 * by the same pattern (the search engine only provides filtered
		 * matches). For the case when the pattern is a camel case pattern with
		 * a terminator, the filter is not set to match everything mode because
		 * jdt.core's SearchPattern does not support that case.
		 */
		String typePattern= typeSearchFilter.getNamePattern();
		int matchRule= typeSearchFilter.getMatchRule();
		typeSearchFilter.setMatchEverythingMode(true);

		try {
			engine.searchAllTypeNames(packPattern == null ? null : packPattern.toCharArray(),
					typeSearchFilter.getPackageFlags(),
					typePattern.toCharArray(),
					matchRule,
					typeSearchFilter.getElementKind(),
					typeSearchFilter.getSearchScope(),
					requestor,
					IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
					progressMonitor);
		} finally {
			typeSearchFilter.setMatchEverythingMode(false);
		}
	}

	@Override
	protected Comparator getItemsComparator() {
		return fTypeItemsComparator;
	}

	@Override
	public String getElementName(Object item) {
		TypeNameMatch type= (TypeNameMatch) item;
		return type.getSimpleTypeName();
	}

	@Override
	protected IStatus validateItem(Object item) {

		if (item == null)
			return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, "", null); //$NON-NLS-1$

		if (fValidator != null) {
			IType type= ((TypeNameMatch) item).getType();
			if (!type.exists()) {
				String qualifiedName= TypeNameMatchLabelProvider.getText((TypeNameMatch) item, TypeNameMatchLabelProvider.SHOW_FULLYQUALIFIED);
				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR, Messages.format(JavaUIMessages.FilteredTypesSelectionDialog_error_type_doesnot_exist, qualifiedName), null);
			}
			Object[] elements= { type };
			return fValidator.validate(elements);
		} else
			return Status.OK_STATUS;
	}

	/**
	 * Sets search scope used when searching for types.
	 *
	 * @param scope
	 *            the new scope
	 */
	private void setSearchScope(IJavaSearchScope scope) {
		fSearchScope= scope;
	}

	/*
	 * We only have to ensure history consistency here since the search engine
	 * takes care of working copies.
	 */
	private static class ConsistencyRunnable implements IRunnableWithProgress {
		@Override
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			if (fgFirstTime) {
				// Join the initialize after load job.
				IJobManager manager= Job.getJobManager();
				manager.join(JavaUI.ID_PLUGIN, monitor);
			}
			OpenTypeHistory history= OpenTypeHistory.getInstance();
			if (fgFirstTime || history.isEmpty()) {
				if (history.needConsistencyCheck()) {
					SubMonitor subMonitor= SubMonitor.convert(monitor,JavaUIMessages.TypeSelectionDialog_progress_consistency, 10 );
					refreshSearchIndices(subMonitor.split(9));
					history.checkConsistency(subMonitor.split(1));
				} else {
					refreshSearchIndices(monitor);
				}
				fgFirstTime= false;
			} else {
				history.checkConsistency(monitor);
			}
		}
		public static boolean needsExecution() {
			OpenTypeHistory history= OpenTypeHistory.getInstance();
			return fgFirstTime || history.isEmpty() || history.needConsistencyCheck();
		}
		private void refreshSearchIndices(IProgressMonitor monitor) throws InvocationTargetException {
			try {
				new SearchEngine().searchAllTypeNames(
						null,
						0,
						// make sure we search a concrete name. This is faster according to Kent
						"_______________".toCharArray(), //$NON-NLS-1$
						SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE,
						IJavaSearchConstants.ENUM,
						SearchEngine.createWorkspaceScope(),
						new TypeNameRequestor() { /* dummy */},
						IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
						monitor);
			} catch (JavaModelException e) {
				throw new InvocationTargetException(e);
			}
		}
	}

	/*
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#reloadCache(boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void reloadCache(boolean checkDuplicates, IProgressMonitor monitor) {
		IProgressMonitor remainingMonitor;
		SubMonitor subMonitor= SubMonitor.convert(monitor, JavaUIMessages.TypeSelectionDialog_progress_consistency, 10);
		if (ConsistencyRunnable.needsExecution()) {

			try {
				ConsistencyRunnable runnable= new ConsistencyRunnable();
				runnable.run(subMonitor.split(1));
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, JavaUIMessages.TypeSelectionDialog_error3Title, JavaUIMessages.TypeSelectionDialog_error3Message);
				close();
				return;
			} catch (InterruptedException e) {
				// cancelled by user
				close();
				return;
			}
			remainingMonitor= subMonitor.split(8);
		} else {
			remainingMonitor= subMonitor;
		}
		super.reloadCache(checkDuplicates, remainingMonitor);
	}

	/*
	 * @see org.eclipse.jdt.ui.dialogs.ITypeSelectionComponent#triggerSearch()
	 */
	@Override
	public void triggerSearch() {
		fTypeFilterVersion++;
		applyFilter();
	}

	private class TypeFiltersPreferencesAction extends Action {

		public TypeFiltersPreferencesAction() {
			super(JavaUIMessages.FilteredTypesSelectionDialog_TypeFiltersPreferencesAction_label);
		}

		@Override
		public void run() {
			String typeFilterID= TypeFilterPreferencePage.TYPE_FILTER_PREF_PAGE_ID;
			PreferencesUtil.createPreferenceDialogOn(getShell(), typeFilterID, new String[] { typeFilterID }, null).open();
			triggerSearch();
		}
	}

	/**
	 * A <code>LabelProvider</code> for (the table of) types.
	 */
	private class TypeItemLabelProvider extends LabelProvider implements ILabelDecorator, IStyledLabelProvider {

		private LocalResourceManager fImageManager;

		private BoldStylerProvider fBoldStylerProvider;

		private Styler fBoldQualifierStyler;

		private final ILabelDecorator labelDecorator;

		public TypeItemLabelProvider(ILabelDecorator labelDecorator) {
			fImageManager= new LocalResourceManager(JFaceResources.getResources());
			this.labelDecorator = labelDecorator;
		}

		/*
		 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
		 */
		@Override
		public void dispose() {
			super.dispose();
			fImageManager.dispose();
			if (fBoldStylerProvider != null) {
				fBoldStylerProvider.dispose();
				fBoldStylerProvider= null;
			}
		}

		@Override
		public Image getImage(Object element) {
			final var internal = getImageInternal(element);
			if (element instanceof TypeNameMatch typeNameMatch) {
				if (labelDecorator != null) {
					final var type = typeNameMatch.getType();
					return labelDecorator.decorateImage(internal, type);
				}
			}
			return internal;
		}

		private Image getImageInternal(Object element) {
			if (!(element instanceof TypeNameMatch typeNameMatch)) {
				return super.getImage(element);
			}
			ImageDescriptor contributedImageDescriptor= fTypeInfoUtil.getContributedImageDescriptor(typeNameMatch);
			if (contributedImageDescriptor == null) {
				return TypeNameMatchLabelProvider.getImage(typeNameMatch, TypeNameMatchLabelProvider.SHOW_TYPE_ONLY);
			} else {
				return fImageManager.create(contributedImageDescriptor);
			}
		}

		@Override
		public String getText(Object element) {
			if (!(element instanceof TypeNameMatch typeNameMatch)) {
				return super.getText(element);
			}
			return BasicElementLabels.getJavaElementName(fTypeInfoUtil.getFullyQualifiedText(typeNameMatch));
		}

		@Override
		public Image decorateImage(Image image, Object element) {
			return image;
		}

		@Override
		public String decorateText(String text, Object element) {
			if (!(element instanceof TypeNameMatch typeNameMatch)) {
				return null;
			}
			return BasicElementLabels.getJavaElementName(fTypeInfoUtil.getFullyQualifiedText(typeNameMatch));
		}

		@Override
		public StyledString getStyledText(Object element) {
			String text= getText(element);
			StyledString string= new StyledString(text);

			int index= text.indexOf(JavaElementLabels.CONCAT_STRING);

			final String namePattern= fFilter != null ? fFilter.getNamePattern() : null;
			if (namePattern != null && !"*".equals(namePattern)) { //$NON-NLS-1$
				String typeName= index == -1 ? text : text.substring(0, index);
				int[] matchingRegions= SearchPattern.getMatchingRegions(namePattern, typeName, fFilter.getMatchRule());
				Strings.markMatchingRegions(string, 0, matchingRegions, getBoldStylerProvider().getBoldStyler());
			}

			if (index != -1) {
				string.setStyle(index, text.length() - index, StyledString.QUALIFIER_STYLER);
				final String packagePattern= fFilter != null ? fFilter.getPackagePattern() : null;
				if (packagePattern != null && !"*".equals(packagePattern)) { //$NON-NLS-1$
					index= index + JavaElementLabels.CONCAT_STRING.length();
					int endIndex= text.indexOf(JavaElementLabels.CONCAT_STRING, index);
					String packageName;
					if (endIndex == -1)
						packageName= text.substring(index);
					else
						packageName= text.substring(index, endIndex);
					int[] matchingRegions= SearchPattern.getMatchingRegions(packagePattern, packageName, fFilter.getPackageFlags());
					Strings.markMatchingRegions(string, index, matchingRegions, getBoldQualifierStyler());
				}
			}
			return string;
		}

		private BoldStylerProvider getBoldStylerProvider() {
			if (fBoldStylerProvider == null) {
				fBoldStylerProvider= new BoldStylerProvider(getDialogArea().getFont());
			}
			return fBoldStylerProvider;
		}

		private Styler getBoldQualifierStyler() {
			if (fBoldQualifierStyler == null) {
				fBoldQualifierStyler= new Styler() {
					@Override
					public void applyStyles(TextStyle textStyle) {
						StyledString.QUALIFIER_STYLER.applyStyles(textStyle);
						getBoldStylerProvider().getBoldStyler().applyStyles(textStyle);
					}
				};
			}
			return fBoldQualifierStyler;
		}
	}

	/**
	 * A <code>LabelProvider</code> for the label showing type details.
	 */
	private static class TypeItemDetailsLabelProvider extends LabelProvider {

		private final TypeInfoUtil fTypeInfoUtil;

		public TypeItemDetailsLabelProvider(TypeInfoUtil typeInfoUtil) {
			fTypeInfoUtil= typeInfoUtil;
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof TypeNameMatch typeNameMatch) {
				return TypeNameMatchLabelProvider.getImage(typeNameMatch, TypeNameMatchLabelProvider.SHOW_TYPE_CONTAINER_ONLY);
			}

			return super.getImage(element);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof TypeNameMatch typeNameMatch) {
				return BasicElementLabels.getJavaElementName(fTypeInfoUtil.getQualificationText(typeNameMatch));
			}

			return super.getText(element);
		}
	}

	private static class TypeInfoUtil {

		private final ITypeInfoImageProvider fProviderExtension;

		private final TypeInfoRequestorAdapter fAdapter= new TypeInfoRequestorAdapter();

		private final Map<IPath, String> fLib2Name= new HashMap<>();

		private final IPath[] fInstallLocations;

		private final String[] fVMNames;

		public TypeInfoUtil(ITypeInfoImageProvider extension) {
			fProviderExtension= extension;
			List<IPath> locations= new ArrayList<>();
			List<String> labels= new ArrayList<>();
			for (IVMInstallType install : JavaRuntime.getVMInstallTypes()) {
				processVMInstallType(install, locations, labels);
			}
			fInstallLocations= CollectionsUtil.toArray(locations, IPath.class);
			fVMNames= labels.toArray(new String[labels.size()]);

		}

		private void processVMInstallType(IVMInstallType installType, List<IPath> locations, List<String> labels) {
			if (installType != null) {
				boolean isMac= Platform.OS.isMac();
				for (IVMInstall install : installType.getVMInstalls()) {
					String label= getFormattedLabel(install.getName());
					LibraryLocation[] libLocations = install.getLibraryLocations();
					if (libLocations != null) {
						processLibraryLocation(libLocations, label);
					} else {
						IPath filePath= Path.fromOSString(install.getInstallLocation().getAbsolutePath());
						// On MacOS X, install locations end in an additional "/Home" segment; remove it.
						if (isMac && "Home".equals(filePath.lastSegment())) //$NON-NLS-1$
							filePath= filePath.removeLastSegments(1);
						locations.add(filePath);
						labels.add(label);
					}
				}
			}
		}

		private void processLibraryLocation(LibraryLocation[] libLocations, String label) {
			for (LibraryLocation location : libLocations) {
				fLib2Name.put(location.getSystemLibraryPath(), label);
			}
		}

		private String getFormattedLabel(String name) {
			return Messages.format(JavaUIMessages.FilteredTypesSelectionDialog_library_name_format, name);
		}

		public String getFullyQualifiedText(TypeNameMatch type) {
			StringBuilder result= new StringBuilder();
			result.append(type.getSimpleTypeName());
			String containerName= type.getTypeContainerName();
			if (containerName.length() > 0) {
				result.append(JavaElementLabels.CONCAT_STRING);
				result.append(containerName);
			}
			result.append(JavaElementLabels.CONCAT_STRING);
			result.append(getContainerName(type));
			return result.toString();
		}

		public String getQualificationText(TypeNameMatch type) {
			StringBuilder result= new StringBuilder();
			String containerName= type.getTypeContainerName();
			if (containerName.length() > 0) {
				result.append(containerName);
				result.append(JavaElementLabels.CONCAT_STRING);
			}
			result.append(getContainerName(type));
			return result.toString();
		}

		public ImageDescriptor getContributedImageDescriptor(TypeNameMatch type) {
			if (fProviderExtension != null) {
				fAdapter.setMatch(type);
				return fProviderExtension.getImageDescriptor(fAdapter);
			}
			return null;
		}

		private String getContainerName(TypeNameMatch type) {
			IPackageFragmentRoot root= type.getPackageFragmentRoot();
			if (root.isExternal()) {
				IPath path= root.getPath();
				for (int i= 0; i < fInstallLocations.length; i++) {
					if (fInstallLocations[i].isPrefixOf(path)) {
						return fVMNames[i];
					}
				}
				String lib= fLib2Name.get(path);
				if (lib != null)
					return lib;
			}
			StringBuffer buf= new StringBuffer();
			JavaElementLabels.getPackageFragmentRootLabel(root, JavaElementLabels.ROOT_QUALIFIED | JavaElementLabels.ROOT_VARIABLE, buf);
			return buf.toString();
		}
	}

	/**
	 * Filters types using pattern, scope, element kind and filter extension.
	 */
	private class TypeItemsFilter extends ItemsFilter {

		private boolean fMatchEverything= false;

		private final int fMyTypeFilterVersion= fTypeFilterVersion;

		private final TypeInfoFilter fTypeInfoFilter;


		public TypeItemsFilter(IJavaSearchScope scope, int elementKind, ITypeInfoFilterExtension extension) {
			/*
			 * Horribly convoluted initialization:
			 * FilteredItemsSelectionDialog.ItemsFilter#ItemsFilter(SearchPattern)
			 * fetches the pattern string from the Text widget of the outer class and
			 * initializes the given SearchPattern with that string.
			 * The default SearchPattern also removes whitespace from the pattern string,
			 * which is why we have to supply our own (dummy) implementation.
			 */
			super(new TypeSearchPattern());
			String pattern= patternMatcher.getPattern();
			fTypeInfoFilter= new TypeInfoFilter(pattern, scope, elementKind, extension);
		}

		@Override
		public boolean isSubFilter(ItemsFilter filter) {
			if (! (filter instanceof TypeItemsFilter typeItemsFilter))
				return false;
			if (fMyTypeFilterVersion != typeItemsFilter.getMyTypeFilterVersion())
				return false;

			//Caveat: This method is defined the wrong way 'round in FilteredItemsSelectionDialog!
			//WRONG (has reverse meaning!): return fTypeInfoFilter.isSubFilter(filter.getPattern());
			return typeItemsFilter.fTypeInfoFilter.isSubFilter(fTypeInfoFilter.getText());
		}

		@Override
		public boolean equalsFilter(ItemsFilter iFilter) {
			if (!(iFilter instanceof TypeItemsFilter typeItemsFilter))
				return false;
			if (! getPattern().equals(typeItemsFilter.getPattern()))
				return false;
			if (getSearchScope() != typeItemsFilter.getSearchScope())
				return false;
			if (fMyTypeFilterVersion != typeItemsFilter.getMyTypeFilterVersion())
				return false;
			return true;
		}

		public int getElementKind() {
			return fTypeInfoFilter.getElementKind();
		}

		public IJavaSearchScope getSearchScope() {
			return fTypeInfoFilter.getSearchScope();
		}

		public int getMyTypeFilterVersion() {
			return fMyTypeFilterVersion;
		}

		public String getNamePattern() {
			return fTypeInfoFilter.getNamePattern();
		}

		public String getPackagePattern() {
			return fTypeInfoFilter.getPackagePattern();
		}

		public int getPackageFlags() {
			return fTypeInfoFilter.getPackageFlags();
		}

		public boolean matchesRawNamePattern(TypeNameMatch type) {
			return fTypeInfoFilter.matchesRawNamePattern(type);
		}

		public boolean matchesFilterExtension(TypeNameMatch type) {
			return fTypeInfoFilter.matchesFilterExtension(type);
		}

		/**
		 * Set filter to "match everything" mode.
		 *
		 * @param matchEverything if <code>true</code>, {@link #matchItem(Object)} always returns true.
		 * 					If <code>false</code>, the filter is enabled.
		 */
		public void setMatchEverythingMode(boolean matchEverything) {
			fMatchEverything= matchEverything;
		}

		@Override
		public boolean isConsistentItem(Object item) {
			return true;
		}

		@Override
		public boolean matchItem(Object item) {
			if (fMatchEverything)
				return true;

			TypeNameMatch type= (TypeNameMatch) item;
			return fTypeInfoFilter.matchesHistoryElement(type);
		}

		@Override
		public boolean matchesRawNamePattern(Object item) {
			TypeNameMatch type= (TypeNameMatch) item;
			return matchesRawNamePattern(type);
		}

		/**
		 * @return search flags
		 * @see org.eclipse.jdt.core.search.SearchPattern#getMatchRule()
		 */
		@Override
		public int getMatchRule() {
			return fTypeInfoFilter.getSearchFlags();
		}

		@Override
		public String getPattern() {
			return fTypeInfoFilter.getText();
		}

		@Override
		public boolean isCamelCasePattern() {
			return fTypeInfoFilter.isCamelCasePattern();
		}

		/**
		 * Matches text with filter.
		 *
		 * @param text the text to match with the filter
		 * @return never returns
		 * @throws UnsupportedOperationException always
		 *
		 * @deprecated not used
		 */
		@Deprecated
		@Override
		protected boolean matches(String text) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Replaces functionality of {@link org.eclipse.ui.dialogs.SearchPattern} with an
	 * adapter implementation that delegates to {@link TypeInfoFilter}.
	 */
	private static class TypeSearchPattern extends org.eclipse.ui.dialogs.SearchPattern {

		private String fPattern;

		@Override
		public void setPattern(String stringPattern) {
			fPattern= stringPattern;
		}

		@Override
		public String getPattern() {
			return fPattern;
		}
	}

	/**
	 * A <code>TypeSearchRequestor</code> collects matches filtered using
	 * <code>TypeItemsFilter</code>. The attached content provider is filled
	 * on the basis of the collected entries (instances of
	 * <code>TypeNameMatch</code>).
	 */
	private static class TypeSearchRequestor extends TypeNameMatchRequestor {
		private volatile boolean fStop;

		private final AbstractContentProvider fContentProvider;

		private final TypeItemsFilter fTypeItemsFilter;

		public TypeSearchRequestor(AbstractContentProvider contentProvider, TypeItemsFilter typeItemsFilter) {
			super();
			fContentProvider= contentProvider;
			fTypeItemsFilter= typeItemsFilter;
		}

		@Override
		public void acceptTypeNameMatch(TypeNameMatch match) {
			if (fStop)
				return;
			if (TypeFilter.isFiltered(match))
				return;
			if (fTypeItemsFilter.matchesFilterExtension(match))
				fContentProvider.add(match, fTypeItemsFilter);
		}

	}

	/**
	 * Compares TypeItems is used during sorting
	 */
	private static class TypeItemsComparator implements Comparator<TypeNameMatch> {

		private final Map<String, String> fLib2Name= new HashMap<>();

		private final String[] fInstallLocations;

		private final String[] fVMNames;

		/**
		 * Creates new instance of TypeItemsComparator
		 */
		public TypeItemsComparator() {
			List<String> locations= new ArrayList<>();
			List<String> labels= new ArrayList<>();
			for (IVMInstallType install : JavaRuntime.getVMInstallTypes()) {
				processVMInstallType(install, locations, labels);
			}
			fInstallLocations= locations.toArray(new String[locations.size()]);
			fVMNames= labels.toArray(new String[labels.size()]);
		}

		private void processVMInstallType(IVMInstallType installType, List<String> locations, List<String> labels) {
			if (installType != null) {
				boolean isMac= Platform.OS.isMac();
				final String HOME_SUFFIX= "/Home"; //$NON-NLS-1$
				for (IVMInstall install : installType.getVMInstalls()) {
					String label = getFormattedLabel(install.getName());
					LibraryLocation[] libLocations= install.getLibraryLocations();
					if (libLocations != null) {
						processLibraryLocation(libLocations, label);
					} else {
						String filePath= install.getInstallLocation().getAbsolutePath();
						// on MacOS X install locations end in an additional
						// "/Home" segment; remove it
						if (isMac && filePath.endsWith(HOME_SUFFIX))
							filePath= filePath.substring(0, filePath.length() - HOME_SUFFIX.length() + 1);
						locations.add(filePath);
						labels.add(label);
					}
				}
			}
		}

		private void processLibraryLocation(LibraryLocation[] libLocations, String label) {
			for (LibraryLocation location : libLocations) {
				fLib2Name.put(location.getSystemLibraryPath().toString(), label);
			}
		}

		private String getFormattedLabel(String name) {
			return MessageFormat.format(JavaUIMessages.FilteredTypesSelectionDialog_library_name_format, name);
		}

		@Override
		public int compare(TypeNameMatch leftInfo, TypeNameMatch rightInfo) {
			int result= compareName(leftInfo.getSimpleTypeName(), rightInfo.getSimpleTypeName());
			if (result != 0)
				return result;

			result= compareDeprecation(leftInfo.getModifiers(), rightInfo.getModifiers());
			if (result != 0)
				return result;

			int leftCategory= getElementTypeCategory(leftInfo);
			int rightCategory= getElementTypeCategory(rightInfo);
			if (leftCategory < rightCategory)
				return -1;
			if (leftCategory > rightCategory)
				return +1;

			result= compareTypeContainerName(leftInfo.getTypeContainerName(), rightInfo.getTypeContainerName());
			if (result != 0)
				return result;

			return compareContainerName(leftInfo, rightInfo);
		}

		private int compareName(String leftString, String rightString) {
			int result= leftString.compareToIgnoreCase(rightString);
			if (result != 0 || rightString.length() == 0) {
				return result;
			} else if (org.eclipse.jdt.internal.core.manipulation.util.Strings.isLowerCase(leftString.charAt(0))
					&& !org.eclipse.jdt.internal.core.manipulation.util.Strings.isLowerCase(rightString.charAt(0))) {
				return +1;
			} else if (org.eclipse.jdt.internal.core.manipulation.util.Strings.isLowerCase(rightString.charAt(0))
					&& !org.eclipse.jdt.internal.core.manipulation.util.Strings.isLowerCase(leftString.charAt(0))) {
				return -1;
			} else {
				return leftString.compareTo(rightString);
			}
		}

		private int compareDeprecation(int leftType, int rightType) {
			boolean rightIsDeprecated= Flags.isDeprecated(rightType);
			if (Flags.isDeprecated(leftType))
				return rightIsDeprecated ? 0 : +1;
			return rightIsDeprecated ? -1 : 0;
		}

		private int compareTypeContainerName(String leftString, String rightString) {
			int leftLength= leftString.length();
			int rightLength= rightString.length();
			if (leftLength == 0 && rightLength > 0)
				return -1;
			if (leftLength == 0 && rightLength == 0)
				return 0;
			if (leftLength > 0 && rightLength == 0)
				return +1;
			return compareName(leftString, rightString);
		}

		private int compareContainerName(TypeNameMatch leftType, TypeNameMatch rightType) {
			return getContainerName(leftType).compareTo(getContainerName(rightType));
		}

		private String getContainerName(TypeNameMatch type) {
			IPackageFragmentRoot root= type.getPackageFragmentRoot();
			if (root.isExternal()) {
				String name= root.getPath().toOSString();
				for (int i= 0; i < fInstallLocations.length; i++) {
					if (name.startsWith(fInstallLocations[i])) {
						return fVMNames[i];
					}
				}
				String lib= fLib2Name.get(name);
				if (lib != null)
					return lib;
			}
			StringBuffer buf= new StringBuffer();
			JavaElementLabels.getPackageFragmentRootLabel(root, JavaElementLabels.ROOT_QUALIFIED | JavaElementLabels.ROOT_VARIABLE, buf);
			return buf.toString();
		}

		private int getElementTypeCategory(TypeNameMatch type) {
			try {
				if (type.getPackageFragmentRoot().getKind() == IPackageFragmentRoot.K_SOURCE)
					return 0;
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			return 1;
		}
	}

	/**
	 * Extends the <code>SelectionHistory</code>, providing support for
	 * <code>OpenTypeHistory</code>.
	 */
	protected class TypeSelectionHistory extends SelectionHistory {

		/**
		 * Creates new instance of TypeSelectionHistory
		 */

		public TypeSelectionHistory() {
			super();
		}

		@Override
		public synchronized void accessed(Object object) {
			super.accessed(object);
		}

		@Override
		public synchronized boolean remove(Object element) {
			OpenTypeHistory.getInstance().remove((TypeNameMatch) element);
			return super.remove(element);
		}

		@Override
		public void load(IMemento memento) {
			TypeNameMatch[] types= OpenTypeHistory.getInstance().getTypeInfos();

			for (int i= types.length - 1; i >= 0 ; i--) { // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=205314
				TypeNameMatch type= types[i];
				accessed(type);
			}
		}

		@Override
		public void save(IMemento memento) {
			persistHistory();
		}

		/**
		 * Stores contents of the local history into persistent history
		 * container.
		 */
		private synchronized void persistHistory() {
			if (getReturnCode() == OK) {
				for (Object item : getHistoryItems()) {
					OpenTypeHistory.getInstance().accessed((TypeNameMatch) item);
				}
			}
		}

		@Override
		protected Object restoreItemFromMemento(IMemento element) {
			return null;
		}

		@Override
		protected void storeItemToMemento(Object item, IMemento element) {
			// not used
		}

	}

}
