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
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class CorrectionMessages extends NLS {

	private static final String BUNDLE_NAME= CorrectionMessages.class.getName();

	private CorrectionMessages() {
		// Do not instantiate
	}

	public static String SerialVersionSubProcessor_createdefault_description;
	public static String SerialVersionSubProcessor_createhashed_description;
	public static String SerialVersionLaunchConfigurationDelegate_setting_up;
	public static String SerialVersionLaunchConfigurationDelegate_starting_vm;
	public static String SerialVersionLaunchConfigurationDelegate_launching_computation;
	public static String SerialVersionLaunchConfigurationDelegate_launching_vm;
	public static String SerialVersionLaunchConfigurationDelegate_verifying_launch_attributes;
	public static String SerialVersionLaunchConfigurationDelegate_constructing_command_line;
	public static String SerialVersionLaunchConfigurationDelegate_temp_file_not_exists;
	public static String SerialVersionLaunchConfigurationDelegate_error_getting_separator_property;
	public static String SerialVersionLaunchConfigurationDelegate_error_getting_temp_dir_property;
	public static String SerialVersionDefaultProposal_message_default_info;
	public static String SerialVersionHashProposal_message_generated_info;
	public static String SerialVersionHashProposal_dialog_error_caption;
	public static String SerialVersionHashProposal_dialog_error_message;
	public static String SerialVersionHashProposal_wrong_launch_delegate;
	public static String SerialVersionHashProposal_wrong_executable;
	public static String SerialVersionHashProposal_computing_id;
	public static String SerialVersionHashProposal_save_caption;
	public static String SerialVersionHashProposal_save_message;
	public static String SerialVersionHashProposal_unable_locate_executable;
	public static String CorrectPackageDeclarationProposal_name;
	public static String CorrectPackageDeclarationProposal_remove_description;
	public static String CorrectPackageDeclarationProposal_add_description;
	public static String CorrectPackageDeclarationProposal_change_description;
	public static String JavaCorrectionProcessor_addquote_description;
	public static String ChangeCorrectionProposal_error_title;
	public static String ChangeCorrectionProposal_error_message;
	public static String ChangeCorrectionProposal_name_with_shortcut;
	public static String CUCorrectionProposal_error_title;
	public static String CUCorrectionProposal_error_message;
	public static String ReorgCorrectionsSubProcessor_renametype_description;
	public static String ReorgCorrectionsSubProcessor_renamecu_description;
	public static String ReorgCorrectionsSubProcessor_movecu_default_description;
	public static String ReorgCorrectionsSubProcessor_movecu_description;
	public static String ReorgCorrectionsSubProcessor_unusedimport_description;
	public static String ReorgCorrectionsSubProcessor_organizeimports_description;
	public static String ReorgCorrectionsSubProcessor_addcp_project_description;
	public static String ReorgCorrectionsSubProcessor_addcp_archive_description;
	public static String ReorgCorrectionsSubProcessor_addcp_classfolder_description;
	public static String ReorgCorrectionsSubProcessor_50_project_compliance_description;
	public static String ReorgCorrectionsSubProcessor_50_workspace_compliance_description;
	public static String ReorgCorrectionsSubProcessor_addcp_variable_description;
	public static String ReorgCorrectionsSubProcessor_addcp_library_description;
	public static String LocalCorrectionsSubProcessor_surroundwith_description;
	public static String LocalCorrectionsSubProcessor_addthrows_description;
	public static String LocalCorrectionsSubProcessor_addadditionalcatch_description;
	public static String LocalCorrectionsSubProcessor_unnecessaryinstanceof_description;
	public static String LocalCorrectionsSubProcessor_unnecessarythrow_description;
	public static String LocalCorrectionsSubProcessor_classtointerface_description;
	public static String LocalCorrectionsSubProcessor_unqualifiedfieldaccess_description;
	public static String LocalCorrectionsSubProcessor_externalizestrings_description;
	public static String LocalCorrectionsSubProcessor_externalizestrings_dialog_title;
	public static String LocalCorrectionsSubProcessor_extendstoimplements_description;
	public static String LocalCorrectionsSubProcessor_addnon_nls_description;
	public static String LocalCorrectionsSubProcessor_changeaccesstostatic_description;
	public static String LocalCorrectionsSubProcessor_changeaccesstostaticdefining_description;
	public static String LocalCorrectionsSubProcessor_setparenteses_bitop_description;
	public static String LocalCorrectionsSubProcessor_indirectaccesstostatic_description;
	public static String LocalCorrectionsSubProcessor_uninitializedvariable_description;
	public static String LocalCorrectionsSubProcessor_removesemicolon_description;
	public static String LocalCorrectionsSubProcessor_removeunreachablecode_description;
	public static String LocalCorrectionsSubProcessor_removeelse_description;
	public static String LocalCorrectionsSubProcessor_hiding_local_label;
	public static String LocalCorrectionsSubProcessor_hiding_field_label;
	public static String LocalCorrectionsSubProcessor_rename_var_label;
	public static String LocalCorrectionsSubProcessor_hiding_argument_label;
	public static String LocalCorrectionsSubProcessor_setparenteses_description;
	public static String LocalCorrectionsSubProcessor_setparenteses_instanceof_description;
	public static String LocalCorrectionsSubProcessor_InferGenericTypeArguments;
	public static String LocalCorrectionsSubProcessor_InferGenericTypeArguments_description;
	public static String TypeMismatchSubProcessor_addcast_description;
	public static String TypeMismatchSubProcessor_changecast_description;
	public static String TypeMismatchSubProcessor_changereturntype_description;
	public static String TypeMismatchSubProcessor_changereturnofoverridden_description;
	public static String TypeMismatchSubProcessor_changereturnofimplemented_description;
	public static String TypeMismatchSubProcessor_removeexceptions_description;
	public static String TypeMismatchSubProcessor_addexceptions_description;
	public static String RemoveDeclarationCorrectionProposal_removeunusedfield_description;
	public static String RemoveDeclarationCorrectionProposal_removeunusedmethod_description;
	public static String RemoveDeclarationCorrectionProposal_removeunusedconstructor_description;
	public static String RemoveDeclarationCorrectionProposal_removeunusedtype_description;
	public static String RemoveDeclarationCorrectionProposal_removeunusedvar_description;
	public static String ModifierCorrectionSubProcessor_changemodifiertostatic_description;
	public static String ModifierCorrectionSubProcessor_changemodifiertononstatic_description;
	public static String ModifierCorrectionSubProcessor_changemodifiertofinal_description;
	public static String ModifierCorrectionSubProcessor_changemodifiertodefault_description;
	public static String ModifierCorrectionSubProcessor_changemodifiertononfinal_description;
	public static String ModifierCorrectionSubProcessor_changevisibility_description;
	public static String ModifierCorrectionSubProcessor_removeabstract_description;
	public static String ModifierCorrectionSubProcessor_removebody_description;
	public static String ModifierCorrectionSubProcessor_default;
	public static String ModifierCorrectionSubProcessor_addabstract_description;
	public static String ModifierCorrectionSubProcessor_removenative_description;
	public static String ModifierCorrectionSubProcessor_addmissingbody_description;
	public static String ModifierCorrectionSubProcessor_setmethodabstract_description;
	public static String ModifierCorrectionSubProcessor_changemethodtononfinal_description;
	public static String ModifierCorrectionSubProcessor_changeoverriddenvisibility_description;
	public static String ModifierCorrectionSubProcessor_changemethodvisibility_description;
	public static String ModifierCorrectionSubProcessor_changemethodtononstatic_description;
	public static String ModifierCorrectionSubProcessor_removeinvalidmodifiers_description;
	public static String ReturnTypeSubProcessor_constrnamemethod_description;
	public static String ReturnTypeSubProcessor_voidmethodreturns_description;
	public static String ReturnTypeSubProcessor_removereturn_description;
	public static String ReturnTypeSubProcessor_missingreturntype_description;
	public static String ReturnTypeSubProcessor_wrongconstructorname_description;
	public static String ReturnTypeSubProcessor_changetovoid_description;
	public static String MissingReturnTypeCorrectionProposal_addreturnstatement_description;
	public static String MissingReturnTypeCorrectionProposal_changereturnstatement_description;
	public static String UnresolvedElementsSubProcessor_swaparguments_description;
	public static String UnresolvedElementsSubProcessor_addargumentcast_description;
	public static String UnresolvedElementsSubProcessor_changemethod_description;
	public static String UnresolvedElementsSubProcessor_changetoouter_description;
	public static String UnresolvedElementsSubProcessor_changetomethod_description;
	public static String UnresolvedElementsSubProcessor_createmethod_description;
	public static String UnresolvedElementsSubProcessor_createmethod_other_description;
	public static String UnresolvedElementsSubProcessor_createconstructor_description;
	public static String UnresolvedElementsSubProcessor_changetype_description;
	public static String UnresolvedElementsSubProcessor_changetype_nopack_description;
	public static String UnresolvedElementsSubProcessor_importtype_description;
	public static String UnresolvedElementsSubProcessor_changevariable_description;
	public static String UnresolvedElementsSubProcessor_createfield_description;
	public static String UnresolvedElementsSubProcessor_createfield_other_description;
	public static String UnresolvedElementsSubProcessor_createlocal_description;
	public static String UnresolvedElementsSubProcessor_createparameter_description;
	public static String UnresolvedElementsSubProcessor_createconst_description;
	public static String UnresolvedElementsSubProcessor_createenum_description;
	public static String UnresolvedElementsSubProcessor_createconst_other_description;
	public static String UnresolvedElementsSubProcessor_removestatement_description;
	public static String UnresolvedElementsSubProcessor_changeparamsignature_description;
	public static String UnresolvedElementsSubProcessor_changemethodtargetcast_description;
	public static String UnresolvedElementsSubProcessor_changeparamsignature_constr_description;
	public static String UnresolvedElementsSubProcessor_swapparams_description;
	public static String UnresolvedElementsSubProcessor_swapparams_constr_description;
	public static String UnresolvedElementsSubProcessor_removeparam_description;
	public static String UnresolvedElementsSubProcessor_removeparams_description;
	public static String UnresolvedElementsSubProcessor_removeparam_constr_description;
	public static String UnresolvedElementsSubProcessor_removeparams_constr_description;
	public static String UnresolvedElementsSubProcessor_addargument_description;
	public static String UnresolvedElementsSubProcessor_addarguments_description;
	public static String UnresolvedElementsSubProcessor_removeargument_description;
	public static String UnresolvedElementsSubProcessor_removearguments_description;
	public static String UnresolvedElementsSubProcessor_addparam_description;
	public static String UnresolvedElementsSubProcessor_addparams_description;
	public static String UnresolvedElementsSubProcessor_addparam_constr_description;
	public static String UnresolvedElementsSubProcessor_addparams_constr_description;
	public static String UnresolvedElementsSubProcessor_importexplicit_description;
	public static String UnresolvedElementsSubProcessor_missingcastbrackets_description;
	public static String UnresolvedElementsSubProcessor_methodtargetcast2_description;
	public static String UnresolvedElementsSubProcessor_changemethodtargetcast2_description;
	public static String UnresolvedElementsSubProcessor_addtypeparameter_method_description;
	public static String UnresolvedElementsSubProcessor_methodtargetcast_description;
	public static String UnresolvedElementsSubProcessor_arraychangetomethod_description;
	public static String UnresolvedElementsSubProcessor_arraychangetolength_description;
	public static String UnresolvedElementsSubProcessor_addnewkeyword_description;
	public static String JavadocTagsSubProcessor_addjavadoc_method_description;
	public static String JavadocTagsSubProcessor_addjavadoc_type_description;
	public static String JavadocTagsSubProcessor_addjavadoc_field_description;
	public static String JavadocTagsSubProcessor_addjavadoc_paramtag_description;
	public static String JavadocTagsSubProcessor_addjavadoc_throwstag_description;
	public static String JavadocTagsSubProcessor_addjavadoc_returntag_description;
	public static String JavadocTagsSubProcessor_addjavadoc_enumconst_description;
	public static String JavadocTagsSubProcessor_addjavadoc_allmissing_description;
	public static String JavadocTagsSubProcessor_removetag_description;
	public static String NoCorrectionProposal_description;
	public static String MarkerResolutionProposal_additionaldesc;
	public static String NewCUCompletionUsingWizardProposal_createclass_description;
	public static String NewCUCompletionUsingWizardProposal_createenum_description;
	public static String NewCUCompletionUsingWizardProposal_createclass_inpackage_description;
	public static String NewCUCompletionUsingWizardProposal_createinnerclass_description;
	public static String NewCUCompletionUsingWizardProposal_createinnerenum_description;
	public static String NewCUCompletionUsingWizardProposal_createannotation_description;
	public static String NewCUCompletionUsingWizardProposal_createinnerclass_intype_description;
	public static String NewCUCompletionUsingWizardProposal_createinnerenum_intype_description;
	public static String NewCUCompletionUsingWizardProposal_createinterface_description;
	public static String NewCUCompletionUsingWizardProposal_createinterface_inpackage_description;
	public static String NewCUCompletionUsingWizardProposal_createinnerinterface_description;
	public static String NewCUCompletionUsingWizardProposal_createenum_inpackage_description;
	public static String NewCUCompletionUsingWizardProposal_createinnerannotation_description;
	public static String NewCUCompletionUsingWizardProposal_createinnerinterface_intype_description;
	public static String NewCUCompletionUsingWizardProposal_createinnerannotation_intype_description;
	public static String NewCUCompletionUsingWizardProposal_createannotation_inpackage_description;
	public static String NewCUCompletionUsingWizardProposal_createclass_info;
	public static String NewCUCompletionUsingWizardProposal_createenum_info;
	public static String NewCUCompletionUsingWizardProposal_createinterface_info;
	public static String NewCUCompletionUsingWizardProposal_createannotation_info;
	public static String UnimplementedMethodsCompletionProposal_description;
	public static String UnimplementedMethodsCompletionProposal_info;
	public static String ConstructorFromSuperclassProposal_description;
	public static String AssignToVariableAssistProposal_assigntolocal_description;
	public static String AssignToVariableAssistProposal_assigntofield_description;
	public static String AssignToVariableAssistProposal_assignparamtofield_description;
	public static String QuickAssistProcessor_catchclausetothrows_description;
	public static String QuickAssistProcessor_removecatchclause_description;
	public static String QuickAssistProcessor_unwrap_ifstatement;
	public static String QuickAssistProcessor_unwrap_whilestatement;
	public static String QuickAssistProcessor_unwrap_forstatement;
	public static String QuickAssistProcessor_unwrap_dostatement;
	public static String QuickAssistProcessor_unwrap_trystatement;
	public static String QuickAssistProcessor_unwrap_anonymous;
	public static String QuickAssistProcessor_unwrap_block;
	public static String QuickAssistProcessor_unwrap_methodinvocation;
	public static String QuickAssistProcessor_splitdeclaration_description;
	public static String QuickAssistProcessor_joindeclaration_description;
	public static String QuickAssistProcessor_addfinallyblock_description;
	public static String QuickAssistProcessor_addelseblock_description;
	public static String QuickAssistProcessor_replacethenwithblock_description;
	public static String QuickAssistProcessor_replaceelsewithblock_description;
	public static String QuickAssistProcessor_replacethenelsewithblock_description;
	public static String QuickAssistProcessor_replacebodywithblock_description;
	public static String QuickAssistProcessor_invertequals_description;
	public static String QuickAssistProcessor_typetoarrayInitializer_description;
	public static String QuickAssistProcessor_createmethodinsuper_description;
	public static String QuickAssistProcessor_forLoop_description;
	public static String QuickAssistProcessor_surround_with_runnable;
	public static String LinkedNamesAssistProposal_proposalinfo;
	public static String LinkedNamesAssistProposal_description;
	public static String QuickTemplateProcessor_surround_label;
	public static String NewCUCompletionUsingWizardProposal_dialogtitle;
	public static String NewCUCompletionUsingWizardProposal_tooltip_enclosingtype;
	public static String NewCUCompletionUsingWizardProposal_tooltip_package;
	public static String JavaCorrectionProcessor_error_quickfix_message;
	public static String JavaCorrectionProcessor_error_status;
	public static String JavaCorrectionProcessor_error_quickassist_message;
	public static String TaskMarkerProposal_description;
	public static String TypeChangeCompletionProposal_field_name;
	public static String TypeChangeCompletionProposal_variable_name;
	public static String TypeChangeCompletionProposal_param_name;
	public static String TypeChangeCompletionProposal_method_name;
	public static String ImplementInterfaceProposal_name;
	public static String AdvancedQuickAssistProcessor_convertToIfElse_description;
	public static String AdvancedQuickAssistProcessor_inverseIf_description;
	public static String AdvancedQuickAssistProcessor_inverseBooleanVariable;
	public static String AdvancedQuickAssistProcessor_castAndAssign;
	public static String AdvancedQuickAssistProcessor_pullNegationUp;
	public static String AdvancedQuickAssistProcessor_joinIfSequence;
	public static String AdvancedQuickAssistProcessor_pickSelectedString;
	public static String AdvancedQuickAssistProcessor_negatedVariableName;
	public static String AdvancedQuickAssistProcessor_pushNegationDown;
	public static String AdvancedQuickAssistProcessor_convertSwitchToIf;
	public static String AdvancedQuickAssistProcessor_inverseIfContinue_description;
	public static String AdvancedQuickAssistProcessor_inverseIfToContinue_description;
	public static String AdvancedQuickAssistProcessor_exchangeInnerAndOuterIfConditions_description;
	public static String AdvancedQuickAssistProcessor_inverseConditions_description;
	public static String AdvancedQuickAssistProcessor_inverseConditionalExpression_description;
	public static String AdvancedQuickAssistProcessor_removeParenthesis_description;
	public static String AdvancedQuickAssistProcessor_replaceIfWithConditional;
	public static String AdvancedQuickAssistProcessor_replaceConditionalWithIf;
	public static String AdvancedQuickAssistProcessor_addParethesis_description;
	public static String AdvancedQuickAssistProcessor_joinWithOuter_description;
	public static String AdvancedQuickAssistProcessor_joinWithInner_description;
	public static String AdvancedQuickAssistProcessor_splitAndCondition_description;
	public static String AdvancedQuickAssistProcessor_joinWithOr_description;
	public static String AdvancedQuickAssistProcessor_splitOrCondition_description;
	public static String AdvancedQuickAssistProcessor_exchangeOperands_description;
	public static String AddTypeParameterProposal_method_label;
	public static String AddTypeParameterProposal_type_label;

	static {
		NLS.initializeMessages(BUNDLE_NAME, CorrectionMessages.class);
	}

	public static String ModifierCorrectionSubProcessor_addoverrideannotation;
	public static String ModifierCorrectionSubProcessor_adddeprecatedannotation;
	public static String LocalCorrectionsSubProcessor_externalizestrings_additional_info;
	public static String AssignToVariableAssistProposal_assigntoexistingfield_description;
	public static String ReorgCorrectionsSubProcessor_50_compliance_operation;
	public static String ReorgCorrectionsSubProcessor_no_50jre_title;
	public static String ReorgCorrectionsSubProcessor_no_50jre_message;
	public static String ReorgCorrectionsSubProcessor_50_compliance_changeworkspace_description;
	public static String ReorgCorrectionsSubProcessor_50_compliance_changeproject_description;
	public static String ReorgCorrectionsSubProcessor_50_compliance_changeProjectJREToDefault_description;
	public static String ReorgCorrectionsSubProcessor_50_compliance_changeWorkspaceJRE_description;
	public static String ReorgCorrectionsSubProcessor_50_compliance_changeProjectJRE_description;
	public static String ModifierCorrectionSubProcessor_default_visibility_label;
	public static String ReorgCorrectionsSubProcessor_configure_buildpath_label;
	public static String ReorgCorrectionsSubProcessor_configure_buildpath_description;
	public static String QuickAssistProcessor_extract_to_local_description;
	public static String ModifierCorrectionSubProcessor_suppress_warnings_initializer_label;
	public static String ModifierCorrectionSubProcessor_suppress_warnings_label;
	public static String ReorgCorrectionsSubProcessor_accessrules_description;
	public static String UnresolvedElementsSubProcessor_change_full_type_description;
	public static String LocalCorrectionsSubProcessor_remove_nls_tag_description;
	public static String LocalCorrectionsSubProcessor_qualify_left_hand_side_description;
	public static String LocalCorrectionsSubProcessor_LocalCorrectionsSubProcessor_qualify_right_hand_side_description;
	public static String CorrectionMessages_add_type_parameters_to_instantiation;
	public static String UnresolvedElementsSubProcessor_UnresolvedElementsSubProcessor_changetoattribute_description;
	public static String UnresolvedElementsSubProcessor_UnresolvedElementsSubProcessor_createattribute_description;
	public static String MissingAnnotationAttributesProposal_add_missing_attributes_label;
	public static String FixCorrectionProposal_HitCtrlEnter_description;
	public static String FixCorrectionProposal_hitCtrlEnter_variable_description;
	public static String CorrectionMarkerResolutionGenerator__multiFixErrorDialog_Titel;
	public static String CorrectionMarkerResolutionGenerator_multiFixErrorDialog_description;
}