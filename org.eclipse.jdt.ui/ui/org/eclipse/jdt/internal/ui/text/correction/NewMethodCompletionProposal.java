/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class NewMethodCompletionProposal extends ASTRewriteCorrectionProposal {

	private ASTNode fNode; // MethodInvocation, ConstructorInvocation, SuperConstructorInvocation, ClassInstanceCreation, SuperMethodInvocation
	private List fArguments;
	private ITypeBinding fSenderBinding;
	private boolean fIsInDifferentCU;
	
	public NewMethodCompletionProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode,  List arguments, ITypeBinding binding, int relevance, Image image) {
		super(label, targetCU, null, relevance, image);
		
		fNode= invocationNode;
		fArguments= arguments;
		fSenderBinding= binding;
	}
		
	protected ASTRewrite getRewrite() throws CoreException {
		
		CompilationUnit astRoot= ASTResolving.findParentCompilationUnit(fNode);
		ASTNode typeDecl= astRoot.findDeclaringNode(fSenderBinding);
		ASTNode newTypeDecl= null;
		if (typeDecl != null) {
			fIsInDifferentCU= false;
			newTypeDecl= typeDecl;
		} else {
			fIsInDifferentCU= true;
			CompilationUnit newRoot= AST.parseCompilationUnit(getCompilationUnit(), true);
			newTypeDecl= newRoot.findDeclaringNode(fSenderBinding.getKey());
		}
		if (newTypeDecl != null) {
			ASTRewrite rewrite= new ASTRewrite(newTypeDecl);
			
			List members;
			if (fSenderBinding.isAnonymous()) {
				members= ((AnonymousClassDeclaration) newTypeDecl).bodyDeclarations();
			} else {
				members= ((TypeDeclaration) newTypeDecl).bodyDeclarations();
			}
			MethodDeclaration newStub= getStub(rewrite, newTypeDecl);
			
			if (isConstructor()) {
				members.add(findConstructorInsertIndex(members), newStub);
			} else if (!fIsInDifferentCU) {
				members.add(findMethodInsertIndex(members, fNode.getStartPosition()), newStub);
			} else {
				members.add(newStub);
			}
			rewrite.markAsInserted(newStub, SELECTION_GROUP_DESC); 
			return rewrite;
		}
		return null;
	}
	
	private boolean isConstructor() {
		return fNode.getNodeType() != ASTNode.METHOD_INVOCATION && fNode.getNodeType() != ASTNode.SUPER_METHOD_INVOCATION;
	}
	
	private MethodDeclaration getStub(ASTRewrite rewrite, ASTNode targetTypeDecl) throws CoreException {
		AST ast= targetTypeDecl.getAST();
		MethodDeclaration decl= ast.newMethodDeclaration();
		
		decl.setConstructor(isConstructor());
		decl.setModifiers(evaluateModifiers(targetTypeDecl));
		decl.setName(ast.newSimpleName(getMethodName()));
		
		List arguments= fArguments;
		List params= decl.parameters();
		
		int nArguments= arguments.size();
		ArrayList names= new ArrayList(nArguments);
		for (int i= 0; i < arguments.size(); i++) {
			Expression elem= (Expression) arguments.get(i);
			SingleVariableDeclaration param= ast.newSingleVariableDeclaration();
			Type type= getParameterType(ast, elem);
			param.setType(type);
			param.setName(ast.newSimpleName(getParameterName(names, elem, type)));
			params.add(param);
		}
		
		Block body= null;

		String bodyStatement= ""; //$NON-NLS-1$
		if (!isConstructor()) {
			Type returnType= evaluateMethodType(ast);
			if (returnType == null) {
				decl.setReturnType(ast.newPrimitiveType(PrimitiveType.VOID));
			} else {
				decl.setReturnType(returnType);
			}
			if (!fSenderBinding.isInterface() && returnType != null) {
				ReturnStatement returnStatement= ast.newReturnStatement();
				returnStatement.setExpression(ASTNodeFactory.newDefaultExpression(ast, returnType, 0));
				bodyStatement= ASTNodes.asFormattedString(returnStatement, 0, String.valueOf('\n'));
			}
		}
		if (!fSenderBinding.isInterface()) {
			body= ast.newBlock();
			String placeHolder= CodeGeneration.getMethodBodyContent(getCompilationUnit(), fSenderBinding.getName(), getMethodName(), isConstructor(), bodyStatement, String.valueOf('\n')); 	
			if (placeHolder != null) {
				ASTNode todoNode= rewrite.createPlaceholder(placeHolder, ASTRewrite.STATEMENT);
				body.statements().add(todoNode);
			}
		}
		decl.setBody(body);

		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		if (settings.createComments && !fSenderBinding.isAnonymous()) {
			String string= CodeGeneration.getMethodComment(getCompilationUnit(), fSenderBinding.getName(), decl, null, String.valueOf('\n'));
			if (string != null) {
				Javadoc javadoc= (Javadoc) rewrite.createPlaceholder(string, ASTRewrite.JAVADOC);
				decl.setJavadoc(javadoc);
			}
		}
		return decl;
	}
			
	private String getParameterName(ArrayList takenNames, Expression argNode, Type type) {
		if (argNode instanceof SimpleName) {
			String name= ((SimpleName) argNode).getIdentifier();
			while (takenNames.contains(name)) {
				name += '1';
			}
			takenNames.add(name);
			return name;
		}
		
		int dim= 0;
		if (type.isArrayType()) {
			ArrayType arrayType= (ArrayType) type;
			dim= arrayType.getDimensions();
			type= arrayType.getElementType();
		}
		String typeName= ASTNodes.asString(type);
		String packName= Signature.getQualifier(typeName);
		
		IJavaProject project= getCompilationUnit().getJavaProject();
		String[] excludedNames= (String[]) takenNames.toArray(new String[takenNames.size()]);
		String[] names= NamingConventions.suggestArgumentNames(project, packName, typeName, dim, excludedNames);
		takenNames.add(names[0]);
		return names[0];
	}
			
	private int findMethodInsertIndex(List decls, int currPos) {
		int nDecls= decls.size();
		for (int i= 0; i < nDecls; i++) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof MethodDeclaration && currPos < curr.getStartPosition() + curr.getLength()) {
				return i + 1;
			}
		}
		return nDecls;
	}
	
	private int findConstructorInsertIndex(List decls) {
		int nDecls= decls.size();
		int lastMethod= 0;
		for (int i= nDecls - 1; i >= 0; i--) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof MethodDeclaration) {
				if (((MethodDeclaration) curr).isConstructor()) {
					return i + 1;
				}
				lastMethod= i;
			}
		}
		return lastMethod;
	}	
	
	private String getMethodName() {
		if (fNode instanceof MethodInvocation) {
			return ((MethodInvocation)fNode).getName().getIdentifier();
		} else if (fNode instanceof SuperMethodInvocation) {
			return ((SuperMethodInvocation)fNode).getName().getIdentifier();
		} else {
			return fSenderBinding.getName(); // name of the class
		}
	}
	
	private int evaluateModifiers(ASTNode targetTypeDecl) {
		if (fSenderBinding.isInterface()) {
			// for interface members copy the modifiers from an existing field
			MethodDeclaration[] methodDecls= ((TypeDeclaration) targetTypeDecl).getMethods();
			if (methodDecls.length > 0) {
				return methodDecls[0].getModifiers();
			}
			return 0;
		}
		if (fNode instanceof MethodInvocation) {
			int modifiers= 0;
			Expression expression= ((MethodInvocation)fNode).getExpression();
			if (expression != null) {
				if (expression instanceof Name && ((Name) expression).resolveBinding().getKind() == IBinding.TYPE) {
					modifiers |= Modifier.STATIC;
				}
			} else if (ASTResolving.isInStaticContext(fNode)) {
				modifiers |= Modifier.STATIC;
			}
			ASTNode node= ASTResolving.findParentType(fNode);
			if (targetTypeDecl.equals(node)) {
				modifiers |= Modifier.PRIVATE;
			} else if (node instanceof AnonymousClassDeclaration) {
				modifiers |= Modifier.PROTECTED;
			} else {
				modifiers |= Modifier.PUBLIC;
			}
			return modifiers;
		}
		return Modifier.PUBLIC;
	}
	
	private Type evaluateMethodType(AST ast) throws CoreException {
		ITypeBinding binding= ASTResolving.guessBindingForReference(fNode);
		if (binding != null) {
			String typeName= addImport(binding);
			return ASTNodeFactory.newType(ast, typeName);			
		}
		return null;
	}
	
	private Type getParameterType(AST ast, Expression elem) throws CoreException {
		ITypeBinding binding= ASTResolving.normalizeTypeBinding(elem.resolveTypeBinding());
		if (binding != null) {
			String typeName= addImport(binding);
			return ASTNodeFactory.newType(ast, typeName);
		}
		return ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
	}

}
