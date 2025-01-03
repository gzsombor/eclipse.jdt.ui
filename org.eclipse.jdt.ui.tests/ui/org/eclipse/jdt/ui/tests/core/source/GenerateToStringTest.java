/*******************************************************************************
 * Copyright (c) 2008, 2024 Mateusz Matela and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - [code manipulation] [dcr] toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=26070
 *     Mateusz Matela <mateusz.matela@gmail.com> - [toString] finish toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=267710
 *     Mateusz Matela <mateusz.matela@gmail.com> - [toString] toString wizard generates wrong code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=270462
 *     Mateusz Matela <mateusz.matela@gmail.com> - [toString] Wrong code generated with String concatenation - https://bugs.eclipse.org/bugs/show_bug.cgi?id=275360
 *     Mateusz Matela <mateusz.matela@gmail.com> - [toString] Generator uses wrong suffixes and prefixes - https://bugs.eclipse.org/bugs/show_bug.cgi?id=275370
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.source;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.GenerateToStringOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.ToStringGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.ToStringGenerationSettingsCore.CustomBuilderSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.ToStringTemplateParser;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class GenerateToStringTest extends SourceTestCase {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	protected ToStringGenerationSettings fSettings2;

	@Before
	public void before() throws CoreException {
		fSettings2= new ToStringGenerationSettings();
		fSettings.setSettings(fSettings2);
		fSettings2.stringFormatTemplate= ToStringTemplateParser.DEFAULT_TEMPLATE;
		fSettings2.toStringStyle= 0;
		fSettings2.skipNulls= false;
		fSettings2.createComments= false;
		fSettings2.customArrayToString= false;
		fSettings2.limitElements= false;
		fSettings2.limitValue= 10;
		fSettings2.useBlocks= true;
		setCompilerLevels();
		fSettings2.customBuilderSettings= new CustomBuilderSettings();
		fSettings2.customBuilderSettings.className= "com.pack.ToStringBuilder";
		fSettings2.customBuilderSettings.variableName= "builder";
		fSettings2.customBuilderSettings.appendMethod= "append";
		fSettings2.customBuilderSettings.resultMethod= "toString";
		fSettings2.customBuilderSettings.chainCalls= false;

		IPackageFragment packageFragment= fRoot.createPackageFragment("com.pack", true, null);
		ICompilationUnit compilationUnit= packageFragment.getCompilationUnit("ToStringBuilder.java");
		compilationUnit
				.createType(
						"package com.pack;\npublic class ToStringBuilder {\npublic ToStringBuilder(Object o){\n}\npublic ToStringBuilder append(String s, Object o){\nreturn null;\n}\npublic String toString(){\nreturn null;\n}\n}\n",
						null, true, null);

		packageFragment= fRoot.createPackageFragment("org.another.pack", true, null);
		compilationUnit= packageFragment.getCompilationUnit("AnotherToStringCreator.java");
		compilationUnit
				.createType(
						"package org.another.pack;\npublic class AnotherToStringCreator {\npublic AnotherToStringCreator(java.lang.Object o) {\n}\npublic AnotherToStringCreator addSth(Object o, String s) {\n return null;\n}\npublic String addSth(String s, int i){\nreturn null;\n}\npublic void addSth(boolean b, String s){\n}\npublic String getResult(){\nreturn null;\n}\n}\n",
						null, true, null);
	}

	private void setCompilerLevels() throws CoreException {
		IJavaProject jp= fRoot.getJavaProject();
		ArrayList<IClasspathEntry> newCP= new ArrayList<>();
		for (IClasspathEntry cpe : jp.getRawClasspath()) {
			if (cpe.getEntryKind() != IClasspathEntry.CPE_LIBRARY) {
				newCP.add(cpe);
			}
		}
		jp.setRawClasspath(newCP.toArray(new IClasspathEntry[newCP.size()]), null);
		JavaProjectHelper.addRTJar18(jp);
	}

	public void runOperation(IType type, IMember[] members, IJavaElement insertBefore) throws CoreException {

		RefactoringASTParser parser= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);

		Object[] fKeys= new Object[members.length];
		for (int i= 0; i < members.length; i++) {
			Assert.assertTrue(members[i].exists());
			if (members[i] instanceof IField) {
				VariableDeclarationFragment frag= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField)members[i], unit);
				fKeys[i]= frag.resolveBinding();
				continue;
			}
			if (members[i] instanceof IMethod) {
				MethodDeclaration decl= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod)members[i], unit);
				fKeys[i]= decl.resolveBinding();
				continue;
			}
			Assert.fail("Member " + members[i] + " not a field nor a method");
		}

		AbstractTypeDeclaration decl= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(type, unit);
		ITypeBinding binding= decl.resolveBinding();
		GenerateToStringOperation op= GenerateToStringOperation.createOperation(binding, fKeys, unit, insertBefore, fSettings2, true, true);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(type.getCompilationUnit());
	}

	private IMember[] getMembers(IType type, String[] names) {
		IMember[] members= new IMember[names.length];
		for (int i= 0; i < members.length; i++) {
			members[i]= type.getField(names[i]);
			if (!members[i].exists())
				members[i]= type.getMethod(names[i], new String[0]);
			Assert.assertTrue(members[i].exists());
		}
		return members;
	}

	/**
	 * Compares source with expected and asserts that no new compile errors have been created.
	 *
	 * @param expected source
	 * @param cu compilation unit
	 * @param oldCUNode the old AST root
	 * @throws Exception if test failed if test failed
	 *
	 * @since 3.5
	 */
	private void compareSourceAssertCompilation(String expected, ICompilationUnit cu, CompilationUnit oldCUNode) throws Exception {
		compareSource(expected, cu.getSource());
		CompilationUnit newCUNode= getCUNode(cu);
		for (IProblem problem : RefactoringAnalyzeUtil.getIntroducedCompileProblems(newCUNode, oldCUNode)) {
			assertFalse(problem.toString(), problem.isError());
		}
	}

	private static CompilationUnit getCUNode(ICompilationUnit cu) throws Exception {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setSource(cu);
		return (CompilationUnit)parser.createAST(null);
	}

	/**
	 * string concatenation - basic functionality and comment
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatComment() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				byte aByte;\r
				char aChar;\r
				int anInt;\r
				double aDouble;\r
				float aFloat;\r
				long aLong;\r
				int aFloatMethod() {\r
					return 3.3;\r
				}\r
				int aStringMethod() {\r
					return "";\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong", "aFloatMethod", "aStringMethod" });
		fSettings2.createComments= true;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "	\r\n"
				+ "	boolean aBool;\r\n"
				+ "	byte aByte;\r\n"
				+ "	char aChar;\r\n"
				+ "	int anInt;\r\n"
				+ "	double aDouble;\r\n"
				+ "	float aFloat;\r\n"
				+ "	long aLong;\r\n"
				+ "	int aFloatMethod() {\r\n"
				+ "		return 3.3;\r\n"
				+ "	}\r\n"
				+ "	int aStringMethod() {\r\n"
				+ "		return \"\";\r\n"
				+ "	}\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		return \"A [aBool=\" + aBool + \", aByte=\" + aByte + \", aChar=\" + aChar + \", anInt=\" + anInt + \", aDouble=\" + aDouble + \", aFloat=\" + aFloat + \", aLong=\" + aLong + \", aFloatMethod()=\" + aFloatMethod() + \", aStringMethod()=\" + aStringMethod() + \"]\";\r\n"
				+ "	}\r\n" + "\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.skipNulls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					return "A [" + (aStringMethod() != null ? "aStringMethod()=" + aStringMethod() + ", " : "") + "aFloatMethod()=" + aFloatMethod() + ", " + (anArrayMethod() != null ? "anArrayMethod()=" + anArrayMethod() + ", " : "") + "aBool=" + aBool + ", " + (aString != null ? "aString=" + aString + ", " : "") + "anInt=" + anInt + "]";\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom array toString without limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArray() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n"
				+ "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] anArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n"
				+ "	java.util.List<Boolean> list;\r\n" + "\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "intArray", "list", "object", "stringArray", "anArrayMethod" });
		fSettings2.customArrayToString= true;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Arrays;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] anArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	java.util.List<Boolean> list;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		return \"A [AArray=\" + Arrays.toString(AArray) + \", aBool=\" + aBool + \", anA=\" + anA + \", floatArray=\" + Arrays.toString(floatArray) + \", intArray=\" + Arrays.toString(intArray) + \", list=\" + list + \", object=\" + object + \", stringArray=\" + Arrays.toString(stringArray) + \", anArrayMethod()=\" + Arrays.toString(anArrayMethod()) + \"]\";\r\n"
				+ "	}\r\n" + "\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - limit of elements but not in arrays
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.limitElements= true;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n"
				+ "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		final int maxLen = 10;\r\n"
				+ "		return \"A [AArray=\" + AArray + \", aBool=\" + aBool + \", anA=\" + anA + \", floatArray=\" + floatArray + \", hashMap=\" + (hashMap != null ? toString(hashMap.entrySet(), maxLen) : null) + \", intArray=\" + intArray + \", integerCollection=\" + (integerCollection != null ? toString(integerCollection, maxLen) : null) + \", list=\" + (list != null ? toString(list, maxLen) : null) + \", object=\" + object + \", stringArray=\" + stringArray + \", wildCollection=\" + (wildCollection != null ? toString(wildCollection, maxLen) : null) + \", charArrayMethod()=\" + charArrayMethod() + \", floatArrayMethod()=\" + floatArrayMethod() + \"]\";\r\n"
				+ "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"[\");\r\n"
				+ "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0) {\r\n" + "				builder.append(\", \");\r\n"
				+ "			}\r\n" + "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom array toString and limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Arrays;\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n"
				+ "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		final int maxLen = 10;\r\n"
				+ "		return \"A [AArray=\" + (AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) : null) + \", aBool=\" + aBool + \", anA=\" + anA + \", floatArray=\" + (floatArray != null ? Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) : null) + \", hashMap=\" + (hashMap != null ? toString(hashMap.entrySet(), maxLen) : null) + \", intArray=\" + (intArray != null ? Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) : null) + \", integerCollection=\" + (integerCollection != null ? toString(integerCollection, maxLen) : null) + \", list=\" + (list != null ? toString(list, maxLen) : null) + \", object=\" + object + \", stringArray=\" + (stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) : null) + \", wildCollection=\" + (wildCollection != null ? toString(wildCollection, maxLen) : null) + \", charArrayMethod()=\"\r\n"
				+ "				+ (charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) : null) + \", floatArrayMethod()=\" + (floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : null) + \"]\";\r\n"
				+ "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"[\");\r\n"
				+ "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0) {\r\n" + "				builder.append(\", \");\r\n"
				+ "			}\r\n" + "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom Array, limit elements to 0
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimitZero() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List list;\r\n" + "	HashMap hashMap;\r\n" + "	Collection wildCollection;\r\n" + "	Collection integerCollection;\r\n" + "	\r\n" + "}\r\n" + "",
				true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.limitValue= 0;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List list;\r\n"
				+ "	HashMap hashMap;\r\n"
				+ "	Collection wildCollection;\r\n"
				+ "	Collection integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		return \"A [AArray=\" + (AArray != null ? \"[]\" : null) + \", aBool=\" + aBool + \", anA=\" + anA + \", floatArray=\" + (floatArray != null ? \"[]\" : null) + \", hashMap=\" + (hashMap != null ? \"[]\" : null) + \", intArray=\" + (intArray != null ? \"[]\" : null) + \", integerCollection=\" + (integerCollection != null ? \"[]\" : null) + \", list=\" + (list != null ? \"[]\" : null) + \", object=\" + object + \", stringArray=\" + (stringArray != null ? \"[]\" : null) + \", wildCollection=\" + (wildCollection != null ? \"[]\" : null) + \", charArrayMethod()=\" + (charArrayMethod() != null ? \"[]\" : null) + \", floatArrayMethod()=\" + (floatArrayMethod() != null ? \"[]\" : null) + \"]\";\r\n"
				+ "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom Array, limit elements to 0, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimitZeroNulls() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List list;\r\n" + "	HashMap hashMap;\r\n" + "	Collection wildCollection;\r\n" + "	Collection integerCollection;\r\n" + "	\r\n" + "}\r\n" + "",
				true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.limitValue= 0;
		fSettings2.skipNulls= true;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List list;\r\n"
				+ "	HashMap hashMap;\r\n"
				+ "	Collection wildCollection;\r\n"
				+ "	Collection integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		return \"A [\" + (AArray != null ? \"AArray=[], \" : \"\") + \"aBool=\" + aBool + \", \" + (anA != null ? \"anA=\" + anA + \", \" : \"\") + (floatArray != null ? \"floatArray=[], \" : \"\") + (hashMap != null ? \"hashMap=[], \" : \"\") + (intArray != null ? \"intArray=[], \" : \"\") + (integerCollection != null ? \"integerCollection=[], \" : \"\") + (list != null ? \"list=[], \" : \"\") + (object != null ? \"object=\" + object + \", \" : \"\") + (stringArray != null ? \"stringArray=[], \" : \"\") + (wildCollection != null ? \"wildCollection=[], \" : \"\") + (charArrayMethod() != null ? \"charArrayMethod()=[], \" : \"\") + (floatArrayMethod() != null ? \"floatArrayMethod()=[]\" : \"\") + \"]\";\r\n"
				+ "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - 'use keyword this' and no one-line blocks
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimitThisNoBlock() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.useBlocks= false;
		fSettings2.useKeywordThis= true;

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Arrays;\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n"
				+ "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		final int maxLen = 10;\r\n"
				+ "		return \"A [AArray=\" + (this.AArray != null ? Arrays.asList(this.AArray).subList(0, Math.min(this.AArray.length, maxLen)) : null) + \", aBool=\" + this.aBool + \", anA=\" + this.anA + \", floatArray=\" + (this.floatArray != null ? Arrays.toString(Arrays.copyOf(this.floatArray, Math.min(this.floatArray.length, maxLen))) : null) + \", hashMap=\" + (this.hashMap != null ? this.toString(this.hashMap.entrySet(), maxLen) : null) + \", intArray=\" + (this.intArray != null ? Arrays.toString(Arrays.copyOf(this.intArray, Math.min(this.intArray.length, maxLen))) : null) + \", integerCollection=\" + (this.integerCollection != null ? this.toString(this.integerCollection, maxLen) : null) + \", list=\" + (this.list != null ? this.toString(this.list, maxLen) : null) + \", object=\" + this.object + \", stringArray=\" + (this.stringArray != null ? Arrays.asList(this.stringArray).subList(0, Math.min(this.stringArray.length, maxLen)) : null) + \", wildCollection=\"\r\n"
				+ "				+ (this.wildCollection != null ? this.toString(this.wildCollection, maxLen) : null) + \", charArrayMethod()=\" + (this.charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(this.charArrayMethod(), Math.min(this.charArrayMethod().length, maxLen))) : null) + \", floatArrayMethod()=\" + (this.floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(this.floatArrayMethod(), Math.min(this.floatArrayMethod().length, maxLen))) : null) + \"]\";\r\n"
				+ "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"[\");\r\n"
				+ "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0)\r\n" + "				builder.append(\", \");\r\n"
				+ "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom array, limit elements, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimitNulls() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + " int anInt;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.skipNulls= true;

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Arrays;\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ " int anInt;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n"
				+ "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		final int maxLen = 10;\r\n"
				+ "		return \"A [\" + (AArray != null ? \"AArray=\" + Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) + \", \" : \"\") + \"aBool=\" + aBool + \", anInt=\" + anInt + \", \" + (anA != null ? \"anA=\" + anA + \", \" : \"\") + (floatArray != null ? \"floatArray=\" + Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) + \", \" : \"\") + (hashMap != null ? \"hashMap=\" + toString(hashMap.entrySet(), maxLen) + \", \" : \"\") + (intArray != null ? \"intArray=\" + Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) + \", \" : \"\") + (integerCollection != null ? \"integerCollection=\" + toString(integerCollection, maxLen) + \", \" : \"\") + (list != null ? \"list=\" + toString(list, maxLen) + \", \" : \"\") + (object != null ? \"object=\" + object + \", \" : \"\") + (stringArray != null ? \"stringArray=\" + Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) + \", \" : \"\")\r\n"
				+ "				+ (wildCollection != null ? \"wildCollection=\" + toString(wildCollection, maxLen) + \", \" : \"\") + (charArrayMethod() != null ? \"charArrayMethod()=\" + Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) + \", \" : \"\") + (floatArrayMethod() != null ? \"floatArrayMethod()=\" + Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : \"\") + \"]\";\r\n"
				+ "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"[\");\r\n"
				+ "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0) {\r\n" + "				builder.append(\", \");\r\n"
				+ "			}\r\n" + "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom array, limit elements, no members require helper method
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimitNoHelpers() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + " int anInt;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "intArray", "list", "object", "stringArray", "charArrayMethod",
				"floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.skipNulls= true;

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Arrays;\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ " int anInt;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n"
				+ "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		final int maxLen = 10;\r\n"
				+ "		return \"A [\" + (AArray != null ? \"AArray=\" + Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) + \", \" : \"\") + \"aBool=\" + aBool + \", anInt=\" + anInt + \", \" + (anA != null ? \"anA=\" + anA + \", \" : \"\") + (floatArray != null ? \"floatArray=\" + Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) + \", \" : \"\") + (intArray != null ? \"intArray=\" + Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) + \", \" : \"\") + (list != null ? \"list=\" + list.subList(0, Math.min(list.size(), maxLen)) + \", \" : \"\") + (object != null ? \"object=\" + object + \", \" : \"\") + (stringArray != null ? \"stringArray=\" + Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) + \", \" : \"\") + (charArrayMethod() != null ? \"charArrayMethod()=\" + Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) + \", \" : \"\")\r\n"
				+ "				+ (floatArrayMethod() != null ? \"floatArrayMethod()=\" + Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : \"\") + \"]\";\r\n"
				+ "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - different template
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatTemplate() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.stringFormatTemplate= "ABCD${object.className}(${object.getClassName})\nEFG\n{\n\t${member.name} == ${member.value}\n\t${otherMembers}\n}(${object.className}|${object.hashCode}|${object.superToString}|${object.identityHashCode})\nGoodbye!";
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					return "ABCDA(" + getClass().getName() + ")\\nEFG\\n{\\n\\taStringMethod == " + aStringMethod() + "\\n\\taFloatMethod == " + aFloatMethod() + "\\n\\tanArrayMethod == " + anArrayMethod() + "\\n\\taBool == " + aBool + "\\n\\taString == " + aString + "\\n\\tanInt == " + anInt + "\\n}(A|" + hashCode() + "|" + super.toString() + "|" + System.identityHashCode(this) + ")\\nGoodbye!";\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - replacing existing toString() and arrayToString(array,int), leaving
	 * toString(Collection)
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatReplace() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				A anA;\r
				float[] floatArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				@Override\r
				public String toString() {\r
					return "A []";\r
				}\r
				private String arrayToString(Object array, int len, int maxLen) {\r
					return array[0].toString();\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					return collection.toString();\r
				}\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "anA", "floatArray", "hashMap", "list", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				A anA;\r
				float[] floatArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return "A [anA=" + anA + ", floatArray=" + (floatArray != null ? Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) : null) + ", hashMap=" + (hashMap != null ? toString(hashMap.entrySet(), maxLen) : null) + ", list=" + (list != null ? toString(list, maxLen) : null) + ", charArrayMethod()=" + (charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) : null) + ", floatArrayMethod()=" + (floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : null) + "]";\r
				}\r
				private String arrayToString(Object array, int len, int maxLen) {\r
					return array[0].toString();\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					return collection.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 1;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A [");\r
					if (aStringMethod() != null) {\r
						builder.append("aStringMethod()=");\r
						builder.append(aStringMethod());\r
						builder.append(", ");\r
					}\r
					builder.append("aFloatMethod()=");\r
					builder.append(aFloatMethod());\r
					builder.append(", ");\r
					if (anArrayMethod() != null) {\r
						builder.append("anArrayMethod()=");\r
						builder.append(anArrayMethod());\r
						builder.append(", ");\r
					}\r
					builder.append("aBool=");\r
					builder.append(aBool);\r
					builder.append(", ");\r
					if (aString != null) {\r
						builder.append("aString=");\r
						builder.append(aString);\r
						builder.append(", ");\r
					}\r
					builder.append("anInt=");\r
					builder.append(anInt);\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array toString without limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArray() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n"
				+ "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] anArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n"
				+ "	java.util.List<Boolean> list;\r\n" + "\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "intArray", "list", "object", "stringArray", "anArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.toStringStyle= 1;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Arrays;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n"
				+ "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] anArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n"
				+ "	java.util.List<Boolean> list;\r\n" + "	@Override\r\n" + "	public String toString() {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n"
				+ "		builder.append(\"A [AArray=\");\r\n" + "		builder.append(Arrays.toString(AArray));\r\n" + "		builder.append(\", aBool=\");\r\n" + "		builder.append(aBool);\r\n"
				+ "		builder.append(\", anA=\");\r\n" + "		builder.append(anA);\r\n" + "		builder.append(\", floatArray=\");\r\n" + "		builder.append(Arrays.toString(floatArray));\r\n"
				+ "		builder.append(\", intArray=\");\r\n" + "		builder.append(Arrays.toString(intArray));\r\n" + "		builder.append(\", list=\");\r\n" + "		builder.append(list);\r\n"
				+ "		builder.append(\", object=\");\r\n" + "		builder.append(object);\r\n" + "		builder.append(\", stringArray=\");\r\n" + "		builder.append(Arrays.toString(stringArray));\r\n"
				+ "		builder.append(\", anArrayMethod()=\");\r\n" + "		builder.append(Arrays.toString(anArrayMethod()));\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n"
				+ "	}\r\n" + "\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - limit of elements but not in arrays
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 1;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.Iterator;\r\n" + "import java.util.List;\r\n" + "\r\n"
				+ "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n" + "		return null;\r\n" + "	}\r\n"
				+ "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n" + "	Collection<Integer> integerCollection;\r\n" + "	@Override\r\n"
				+ "	public String toString() {\r\n" + "		final int maxLen = 10;\r\n" + "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"A [AArray=\");\r\n"
				+ "		builder.append(AArray);\r\n" + "		builder.append(\", aBool=\");\r\n" + "		builder.append(aBool);\r\n" + "		builder.append(\", anA=\");\r\n" + "		builder.append(anA);\r\n"
				+ "		builder.append(\", floatArray=\");\r\n" + "		builder.append(floatArray);\r\n" + "		builder.append(\", hashMap=\");\r\n"
				+ "		builder.append(hashMap != null ? toString(hashMap.entrySet(), maxLen) : null);\r\n" + "		builder.append(\", intArray=\");\r\n" + "		builder.append(intArray);\r\n"
				+ "		builder.append(\", integerCollection=\");\r\n" + "		builder.append(integerCollection != null ? toString(integerCollection, maxLen) : null);\r\n"
				+ "		builder.append(\", list=\");\r\n" + "		builder.append(list != null ? toString(list, maxLen) : null);\r\n" + "		builder.append(\", object=\");\r\n"
				+ "		builder.append(object);\r\n" + "		builder.append(\", stringArray=\");\r\n" + "		builder.append(stringArray);\r\n" + "		builder.append(\", wildCollection=\");\r\n"
				+ "		builder.append(wildCollection != null ? toString(wildCollection, maxLen) : null);\r\n" + "		builder.append(\", charArrayMethod()=\");\r\n"
				+ "		builder.append(charArrayMethod());\r\n" + "		builder.append(\", floatArrayMethod()=\");\r\n" + "		builder.append(floatArrayMethod());\r\n" + "		builder.append(\"]\");\r\n"
				+ "		return builder.toString();\r\n" + "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n"
				+ "		builder.append(\"[\");\r\n" + "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0) {\r\n"
				+ "				builder.append(\", \");\r\n" + "			}\r\n" + "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n"
				+ "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array toString and limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 1;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Arrays;\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n" + "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	@Override\r\n" + "	public String toString() {\r\n" + "		final int maxLen = 10;\r\n"
				+ "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"A [AArray=\");\r\n"
				+ "		builder.append(AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) : null);\r\n" + "		builder.append(\", aBool=\");\r\n"
				+ "		builder.append(aBool);\r\n" + "		builder.append(\", anA=\");\r\n" + "		builder.append(anA);\r\n" + "		builder.append(\", floatArray=\");\r\n"
				+ "		builder.append(floatArray != null ? Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) : null);\r\n" + "		builder.append(\", hashMap=\");\r\n"
				+ "		builder.append(hashMap != null ? toString(hashMap.entrySet(), maxLen) : null);\r\n" + "		builder.append(\", intArray=\");\r\n"
				+ "		builder.append(intArray != null ? Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) : null);\r\n" + "		builder.append(\", integerCollection=\");\r\n"
				+ "		builder.append(integerCollection != null ? toString(integerCollection, maxLen) : null);\r\n" + "		builder.append(\", list=\");\r\n"
				+ "		builder.append(list != null ? toString(list, maxLen) : null);\r\n" + "		builder.append(\", object=\");\r\n" + "		builder.append(object);\r\n"
				+ "		builder.append(\", stringArray=\");\r\n" + "		builder.append(stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) : null);\r\n"
				+ "		builder.append(\", wildCollection=\");\r\n" + "		builder.append(wildCollection != null ? toString(wildCollection, maxLen) : null);\r\n"
				+ "		builder.append(\", charArrayMethod()=\");\r\n"
				+ "		builder.append(charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) : null);\r\n"
				+ "		builder.append(\", floatArrayMethod()=\");\r\n"
				+ "		builder.append(floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : null);\r\n"
				+ "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n"
				+ "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"[\");\r\n" + "		int i = 0;\r\n"
				+ "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0) {\r\n" + "				builder.append(\", \");\r\n" + "			}\r\n"
				+ "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit of elements, skip nulls, use keyword this, no one-line
	 * blocks
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimitNullsThisNoBlocks() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.skipNulls= true;
		fSettings2.limitElements= true;
		fSettings2.useBlocks= false;
		fSettings2.useKeywordThis= true;
		fSettings2.toStringStyle= 1;
		fSettings2.stringFormatTemplate= "${object.className}[ ${member.value}, ${otherMembers}]";

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Arrays;\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n" + "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	@Override\r\n" + "	public String toString() {\r\n" + "		final int maxLen = 10;\r\n"
				+ "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"A[ \");\r\n" + "		if (this.AArray != null) {\r\n"
				+ "			builder.append(Arrays.asList(this.AArray).subList(0, Math.min(this.AArray.length, maxLen)));\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n"
				+ "		builder.append(this.aBool);\r\n" + "		builder.append(\", \");\r\n" + "		if (this.anA != null) {\r\n" + "			builder.append(this.anA);\r\n" + "			builder.append(\", \");\r\n"
				+ "		}\r\n" + "		if (this.floatArray != null) {\r\n" + "			builder.append(Arrays.toString(Arrays.copyOf(this.floatArray, Math.min(this.floatArray.length, maxLen))));\r\n"
				+ "			builder.append(\", \");\r\n" + "		}\r\n" + "		if (this.hashMap != null) {\r\n" + "			builder.append(this.toString(this.hashMap.entrySet(), maxLen));\r\n"
				+ "			builder.append(\", \");\r\n" + "		}\r\n" + "		if (this.intArray != null) {\r\n"
				+ "			builder.append(Arrays.toString(Arrays.copyOf(this.intArray, Math.min(this.intArray.length, maxLen))));\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n"
				+ "		if (this.integerCollection != null) {\r\n" + "			builder.append(this.toString(this.integerCollection, maxLen));\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n"
				+ "		if (this.list != null) {\r\n" + "			builder.append(this.toString(this.list, maxLen));\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n" + "		if (this.object != null) {\r\n"
				+ "			builder.append(this.object);\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n" + "		if (this.stringArray != null) {\r\n"
				+ "			builder.append(Arrays.asList(this.stringArray).subList(0, Math.min(this.stringArray.length, maxLen)));\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n"
				+ "		if (this.wildCollection != null) {\r\n" + "			builder.append(this.toString(this.wildCollection, maxLen));\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n"
				+ "		if (this.charArrayMethod() != null) {\r\n" + "			builder.append(Arrays.toString(Arrays.copyOf(this.charArrayMethod(), Math.min(this.charArrayMethod().length, maxLen))));\r\n"
				+ "			builder.append(\", \");\r\n" + "		}\r\n" + "		if (this.floatArrayMethod() != null)\r\n"
				+ "			builder.append(Arrays.toString(Arrays.copyOf(this.floatArrayMethod(), Math.min(this.floatArrayMethod().length, maxLen))));\r\n" + "		builder.append(\"]\");\r\n"
				+ "		return builder.toString();\r\n" + "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n"
				+ "		builder.append(\"[\");\r\n" + "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0)\r\n"
				+ "				builder.append(\", \");\r\n" + "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n"
				+ "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit elements, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimitNulls() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + " int anInt;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 1;

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Arrays;\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + " int anInt;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n" + "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	@Override\r\n" + "	public String toString() {\r\n" + "		final int maxLen = 10;\r\n"
				+ "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"A [\");\r\n" + "		if (AArray != null) {\r\n" + "			builder.append(\"AArray=\");\r\n"
				+ "			builder.append(Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)));\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n" + "		builder.append(\"aBool=\");\r\n"
				+ "		builder.append(aBool);\r\n" + "		builder.append(\", anInt=\");\r\n" + "		builder.append(anInt);\r\n" + "		builder.append(\", \");\r\n" + "		if (anA != null) {\r\n"
				+ "			builder.append(\"anA=\");\r\n" + "			builder.append(anA);\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n" + "		if (floatArray != null) {\r\n"
				+ "			builder.append(\"floatArray=\");\r\n" + "			builder.append(Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))));\r\n"
				+ "			builder.append(\", \");\r\n" + "		}\r\n" + "		if (hashMap != null) {\r\n" + "			builder.append(\"hashMap=\");\r\n"
				+ "			builder.append(toString(hashMap.entrySet(), maxLen));\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n" + "		if (intArray != null) {\r\n"
				+ "			builder.append(\"intArray=\");\r\n" + "			builder.append(Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))));\r\n" + "			builder.append(\", \");\r\n"
				+ "		}\r\n" + "		if (integerCollection != null) {\r\n" + "			builder.append(\"integerCollection=\");\r\n" + "			builder.append(toString(integerCollection, maxLen));\r\n"
				+ "			builder.append(\", \");\r\n" + "		}\r\n" + "		if (list != null) {\r\n" + "			builder.append(\"list=\");\r\n" + "			builder.append(toString(list, maxLen));\r\n"
				+ "			builder.append(\", \");\r\n" + "		}\r\n" + "		if (object != null) {\r\n" + "			builder.append(\"object=\");\r\n" + "			builder.append(object);\r\n"
				+ "			builder.append(\", \");\r\n" + "		}\r\n" + "		if (stringArray != null) {\r\n" + "			builder.append(\"stringArray=\");\r\n"
				+ "			builder.append(Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)));\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n"
				+ "		if (wildCollection != null) {\r\n" + "			builder.append(\"wildCollection=\");\r\n" + "			builder.append(toString(wildCollection, maxLen));\r\n" + "			builder.append(\", \");\r\n"
				+ "		}\r\n" + "		if (charArrayMethod() != null) {\r\n" + "			builder.append(\"charArrayMethod()=\");\r\n"
				+ "			builder.append(Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))));\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n"
				+ "		if (floatArrayMethod() != null) {\r\n" + "			builder.append(\"floatArrayMethod()=\");\r\n"
				+ "			builder.append(Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))));\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n"
				+ "		return builder.toString();\r\n" + "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n"
				+ "		builder.append(\"[\");\r\n" + "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0) {\r\n"
				+ "				builder.append(\", \");\r\n" + "			}\r\n" + "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n"
				+ "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit elements, no members require helper methods
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimitNoHelpers() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + " int anInt;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "intArray", "list", "object", "stringArray", "charArrayMethod",
				"floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 1;

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Arrays;\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n" + "\r\n"
				+ "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + " int anInt;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	@Override\r\n" + "	public String toString() {\r\n" + "		final int maxLen = 10;\r\n"
				+ "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"A [AArray=\");\r\n"
				+ "		builder.append(AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) : null);\r\n" + "		builder.append(\", aBool=\");\r\n"
				+ "		builder.append(aBool);\r\n" + "		builder.append(\", anInt=\");\r\n" + "		builder.append(anInt);\r\n" + "		builder.append(\", anA=\");\r\n" + "		builder.append(anA);\r\n"
				+ "		builder.append(\", floatArray=\");\r\n" + "		builder.append(floatArray != null ? Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) : null);\r\n"
				+ "		builder.append(\", intArray=\");\r\n" + "		builder.append(intArray != null ? Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) : null);\r\n"
				+ "		builder.append(\", list=\");\r\n" + "		builder.append(list != null ? list.subList(0, Math.min(list.size(), maxLen)) : null);\r\n" + "		builder.append(\", object=\");\r\n"
				+ "		builder.append(object);\r\n" + "		builder.append(\", stringArray=\");\r\n"
				+ "		builder.append(stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) : null);\r\n" + "		builder.append(\", charArrayMethod()=\");\r\n"
				+ "		builder.append(charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) : null);\r\n"
				+ "		builder.append(\", floatArrayMethod()=\");\r\n"
				+ "		builder.append(floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : null);\r\n"
				+ "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit elements to 0
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimitZero() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + " int anInt;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.limitValue= 0;
		fSettings2.toStringStyle= 1;

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n"
				+ "	boolean aBool;\r\n" + " int anInt;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n" + "		return null;\r\n" + "	}\r\n"
				+ "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n" + "	Collection<Integer> integerCollection;\r\n" + "	@Override\r\n"
				+ "	public String toString() {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"A [AArray=\");\r\n"
				+ "		builder.append(AArray != null ? \"[]\" : null);\r\n" + "		builder.append(\", aBool=\");\r\n" + "		builder.append(aBool);\r\n" + "		builder.append(\", anInt=\");\r\n"
				+ "		builder.append(anInt);\r\n" + "		builder.append(\", anA=\");\r\n" + "		builder.append(anA);\r\n" + "		builder.append(\", floatArray=\");\r\n"
				+ "		builder.append(floatArray != null ? \"[]\" : null);\r\n" + "		builder.append(\", hashMap=\");\r\n" + "		builder.append(hashMap != null ? \"[]\" : null);\r\n"
				+ "		builder.append(\", intArray=\");\r\n" + "		builder.append(intArray != null ? \"[]\" : null);\r\n" + "		builder.append(\", integerCollection=\");\r\n"
				+ "		builder.append(integerCollection != null ? \"[]\" : null);\r\n" + "		builder.append(\", list=\");\r\n" + "		builder.append(list != null ? \"[]\" : null);\r\n"
				+ "		builder.append(\", object=\");\r\n" + "		builder.append(object);\r\n" + "		builder.append(\", stringArray=\");\r\n" + "		builder.append(stringArray != null ? \"[]\" : null);\r\n"
				+ "		builder.append(\", wildCollection=\");\r\n" + "		builder.append(wildCollection != null ? \"[]\" : null);\r\n" + "		builder.append(\", charArrayMethod()=\");\r\n"
				+ "		builder.append(charArrayMethod() != null ? \"[]\" : null);\r\n" + "		builder.append(\", floatArrayMethod()=\");\r\n"
				+ "		builder.append(floatArrayMethod() != null ? \"[]\" : null);\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit elements to 0, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimitZeroNulls() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + " int anInt;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.limitValue= 0;
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 1;

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n"
				+ "	boolean aBool;\r\n" + " int anInt;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n" + "		return null;\r\n" + "	}\r\n"
				+ "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n" + "	Collection<Integer> integerCollection;\r\n" + "	@Override\r\n"
				+ "	public String toString() {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"A [\");\r\n" + "		if (AArray != null) {\r\n"
				+ "			builder.append(\"AArray=[], \");\r\n" + "		}\r\n" + "		builder.append(\"aBool=\");\r\n" + "		builder.append(aBool);\r\n" + "		builder.append(\", anInt=\");\r\n"
				+ "		builder.append(anInt);\r\n" + "		builder.append(\", \");\r\n" + "		if (anA != null) {\r\n" + "			builder.append(\"anA=\");\r\n" + "			builder.append(anA);\r\n"
				+ "			builder.append(\", \");\r\n" + "		}\r\n" + "		if (floatArray != null) {\r\n" + "			builder.append(\"floatArray=[], \");\r\n" + "		}\r\n" + "		if (hashMap != null) {\r\n"
				+ "			builder.append(\"hashMap=[], \");\r\n" + "		}\r\n" + "		if (intArray != null) {\r\n" + "			builder.append(\"intArray=[], \");\r\n" + "		}\r\n"
				+ "		if (integerCollection != null) {\r\n" + "			builder.append(\"integerCollection=[], \");\r\n" + "		}\r\n" + "		if (list != null) {\r\n" + "			builder.append(\"list=[], \");\r\n"
				+ "		}\r\n" + "		if (object != null) {\r\n" + "			builder.append(\"object=\");\r\n" + "			builder.append(object);\r\n" + "			builder.append(\", \");\r\n" + "		}\r\n"
				+ "		if (stringArray != null) {\r\n" + "			builder.append(\"stringArray=[], \");\r\n" + "		}\r\n" + "		if (wildCollection != null) {\r\n"
				+ "			builder.append(\"wildCollection=[], \");\r\n" + "		}\r\n" + "		if (charArrayMethod() != null) {\r\n" + "			builder.append(\"charArrayMethod()=[], \");\r\n" + "		}\r\n"
				+ "		if (floatArrayMethod() != null) {\r\n" + "			builder.append(\"floatArrayMethod()=[]\");\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n"
				+ "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder, chained calls - skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedBuilderNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 2;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A [");\r
					if (aStringMethod() != null) {\r
						builder.append("aStringMethod()=").append(aStringMethod()).append(", ");\r
					}\r
					builder.append("aFloatMethod()=").append(aFloatMethod()).append(", ");\r
					if (anArrayMethod() != null) {\r
						builder.append("anArrayMethod()=").append(anArrayMethod()).append(", ");\r
					}\r
					builder.append("aBool=").append(aBool).append(", ");\r
					if (aString != null) {\r
						builder.append("aString=").append(aString).append(", ");\r
					}\r
					builder.append("anInt=").append(anInt).append("]");\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder, chained calls - custom array toString without limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedBuilderArray() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n"
				+ "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] anArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n"
				+ "	java.util.List<Boolean> list;\r\n" + "\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "intArray", "list", "object", "stringArray", "anArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.toStringStyle= 2;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Arrays;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] anArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	java.util.List<Boolean> list;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		StringBuilder builder = new StringBuilder();\r\n"
				+ "		builder.append(\"A [AArray=\").append(Arrays.toString(AArray)).append(\", aBool=\").append(aBool).append(\", anA=\").append(anA).append(\", floatArray=\").append(Arrays.toString(floatArray)).append(\", intArray=\").append(Arrays.toString(intArray)).append(\", list=\").append(list).append(\", object=\").append(object).append(\", stringArray=\").append(Arrays.toString(stringArray)).append(\", anArrayMethod()=\").append(Arrays.toString(anArrayMethod())).append(\"]\");\r\n"
				+ "		return builder.toString();\r\n" + "	}\r\n" + "\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder, chained calls - custom array toString without limit of elements, unique names
	 * needed
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedBuilderArrayUnique() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	int[] intArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	List list;\r\n"
				+ "	HashMap hashMap;\r\n" + "	Collection wildCollection;\r\n" + "	Collection integerCollection;\r\n" + "	Object builder;\r\n" + "	Object buffer;\r\n" + "	Object maxLen;\r\n"
				+ "	Object len;\r\n" + "	Object collection;\r\n" + "	Object array;\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aBool", "intArray", "stringArray", "AArray", "list", "hashMap", "wildCollection", "integerCollection", "builder", "buffer",
				"maxLen", "len", "collection", "array" });
		fSettings2.customArrayToString= true;
		fSettings2.toStringStyle= 2;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Arrays;\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	List list;\r\n"
				+ "	HashMap hashMap;\r\n"
				+ "	Collection wildCollection;\r\n"
				+ "	Collection integerCollection;\r\n"
				+ "	Object builder;\r\n"
				+ "	Object buffer;\r\n"
				+ "	Object maxLen;\r\n"
				+ "	Object len;\r\n"
				+ "	Object collection;\r\n"
				+ "	Object array;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		StringBuilder builder2 = new StringBuilder();\r\n"
				+ "		builder2.append(\"A [aBool=\").append(aBool).append(\", intArray=\").append(Arrays.toString(intArray)).append(\", stringArray=\").append(Arrays.toString(stringArray)).append(\", AArray=\").append(Arrays.toString(AArray)).append(\", list=\").append(list).append(\", hashMap=\").append(hashMap).append(\", wildCollection=\").append(wildCollection).append(\", integerCollection=\").append(integerCollection).append(\", builder=\").append(builder).append(\", buffer=\").append(buffer).append(\", maxLen=\").append(maxLen).append(\", len=\").append(len).append(\", collection=\").append(collection).append(\", array=\").append(array).append(\"]\");\r\n"
				+ "		return builder2.toString();\r\n" + "	}\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder, chained calls - custom array, JDK 1.5 compatybility
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedBuilderArray1_5() throws Exception {
		setCompilerLevels();
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.toStringStyle= 2;
		fSettings2.stringFormatTemplate= "${object.className}[ ${member.value}, ${otherMembers}]";

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Arrays;\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n"
				+ "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		StringBuilder builder = new StringBuilder();\r\n"
				+ "		builder.append(\"A[ \").append(Arrays.toString(AArray)).append(\", \").append(aBool).append(\", \").append(anA).append(\", \").append(Arrays.toString(floatArray)).append(\", \").append(hashMap).append(\", \").append(Arrays.toString(intArray)).append(\", \").append(integerCollection).append(\", \").append(list).append(\", \").append(object).append(\", \").append(Arrays.toString(stringArray)).append(\", \").append(wildCollection).append(\", \").append(Arrays.toString(charArrayMethod())).append(\", \").append(Arrays.toString(floatArrayMethod())).append(\"]\");\r\n"
				+ "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder, chained calls - custom array, limit of elements, skip nulls, use keyword
	 * this, no one-line blocks
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedBuilderArrayLimitNullsThisNoBlocks() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.skipNulls= true;
		fSettings2.limitElements= true;
		fSettings2.useBlocks= false;
		fSettings2.useKeywordThis= true;
		fSettings2.toStringStyle= 2;
		fSettings2.stringFormatTemplate= "${object.className}[ ${member.value}, ${otherMembers}]";

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Arrays;\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n" + "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	@Override\r\n" + "	public String toString() {\r\n" + "		final int maxLen = 10;\r\n"
				+ "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"A[ \");\r\n" + "		if (this.AArray != null)\r\n"
				+ "			builder.append(Arrays.asList(this.AArray).subList(0, Math.min(this.AArray.length, maxLen))).append(\", \");\r\n" + "		builder.append(this.aBool).append(\", \");\r\n"
				+ "		if (this.anA != null)\r\n" + "			builder.append(this.anA).append(\", \");\r\n" + "		if (this.floatArray != null)\r\n"
				+ "			builder.append(Arrays.toString(Arrays.copyOf(this.floatArray, Math.min(this.floatArray.length, maxLen)))).append(\", \");\r\n" + "		if (this.hashMap != null)\r\n"
				+ "			builder.append(this.toString(this.hashMap.entrySet(), maxLen)).append(\", \");\r\n" + "		if (this.intArray != null)\r\n"
				+ "			builder.append(Arrays.toString(Arrays.copyOf(this.intArray, Math.min(this.intArray.length, maxLen)))).append(\", \");\r\n" + "		if (this.integerCollection != null)\r\n"
				+ "			builder.append(this.toString(this.integerCollection, maxLen)).append(\", \");\r\n" + "		if (this.list != null)\r\n"
				+ "			builder.append(this.toString(this.list, maxLen)).append(\", \");\r\n" + "		if (this.object != null)\r\n" + "			builder.append(this.object).append(\", \");\r\n"
				+ "		if (this.stringArray != null)\r\n" + "			builder.append(Arrays.asList(this.stringArray).subList(0, Math.min(this.stringArray.length, maxLen))).append(\", \");\r\n"
				+ "		if (this.wildCollection != null)\r\n" + "			builder.append(this.toString(this.wildCollection, maxLen)).append(\", \");\r\n" + "		if (this.charArrayMethod() != null)\r\n"
				+ "			builder.append(Arrays.toString(Arrays.copyOf(this.charArrayMethod(), Math.min(this.charArrayMethod().length, maxLen)))).append(\", \");\r\n"
				+ "		if (this.floatArrayMethod() != null)\r\n" + "			builder.append(Arrays.toString(Arrays.copyOf(this.floatArrayMethod(), Math.min(this.floatArrayMethod().length, maxLen))));\r\n"
				+ "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n"
				+ "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"[\");\r\n" + "		int i = 0;\r\n"
				+ "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0)\r\n" + "				builder.append(\", \");\r\n"
				+ "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - basic test
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void format() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.toStringStyle= 3;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					return String.format("A [aStringMethod()=%s, aFloatMethod()=%s, anArrayMethod()=%s, aBool=%s, aString=%s, anInt=%s]", aStringMethod(), aFloatMethod(), anArrayMethod(), aBool, aString, anInt);\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - custom array toString without limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatArray() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n"
				+ "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] anArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n"
				+ "	java.util.List<Boolean> list;\r\n" + "\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "intArray", "list", "object", "stringArray", "anArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.toStringStyle= 3;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Arrays;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] anArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	java.util.List<Boolean> list;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		return String.format(\"A [AArray=%s, aBool=%s, anA=%s, floatArray=%s, intArray=%s, list=%s, object=%s, stringArray=%s, anArrayMethod()=%s]\", Arrays.toString(AArray), aBool, anA, Arrays.toString(floatArray), Arrays.toString(intArray), list, object, Arrays.toString(stringArray), Arrays.toString(anArrayMethod()));\r\n"
				+ "	}\r\n" + "\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - limit of elements but not arrays
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 3;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n"
				+ "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		final int maxLen = 10;\r\n"
				+ "		return String.format(\"A [AArray=%s, aBool=%s, anA=%s, floatArray=%s, hashMap=%s, intArray=%s, integerCollection=%s, list=%s, object=%s, stringArray=%s, wildCollection=%s, charArrayMethod()=%s, floatArrayMethod()=%s]\", AArray, aBool, anA, floatArray, hashMap != null ? toString(hashMap.entrySet(), maxLen) : null, intArray, integerCollection != null ? toString(integerCollection, maxLen) : null, list != null ? toString(list, maxLen) : null, object, stringArray, wildCollection != null ? toString(wildCollection, maxLen) : null, charArrayMethod(), floatArrayMethod());\r\n"
				+ "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"[\");\r\n"
				+ "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0) {\r\n" + "				builder.append(\", \");\r\n"
				+ "			}\r\n" + "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - custom array toString and limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatArrayLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 3;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Arrays;\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n"
				+ "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		final int maxLen = 10;\r\n"
				+ "		return String.format(\"A [AArray=%s, aBool=%s, anA=%s, floatArray=%s, hashMap=%s, intArray=%s, integerCollection=%s, list=%s, object=%s, stringArray=%s, wildCollection=%s, charArrayMethod()=%s, floatArrayMethod()=%s]\", AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) : null, aBool, anA, floatArray != null ? Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) : null, hashMap != null ? toString(hashMap.entrySet(), maxLen) : null, intArray != null ? Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) : null, integerCollection != null ? toString(integerCollection, maxLen) : null, list != null ? toString(list, maxLen) : null, object, stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) : null, wildCollection != null ? toString(wildCollection, maxLen) : null,\r\n"
				+ "				charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) : null, floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : null);\r\n"
				+ "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"[\");\r\n"
				+ "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0) {\r\n" + "				builder.append(\", \");\r\n"
				+ "			}\r\n" + "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - custom array toString, limit of elements, JDK 1.5, no members require
	 * helper methods
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatArrayLimit1_5NoHelpers() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "list", "object", "stringArray" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 3;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Arrays;\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n"
				+ "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		final int maxLen = 10;\r\n"
				+ "		return String.format(\"A [AArray=%s, aBool=%s, anA=%s, list=%s, object=%s, stringArray=%s]\", AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) : null, aBool, anA, list != null ? list.subList(0, Math.min(list.size(), maxLen)) : null, object, stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) : null);\r\n"
				+ "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - custom array toString and limit number of elements to 0
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatArrayLimitZero() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.limitValue= 0;
		fSettings2.toStringStyle= 3;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n"
				+ "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		return String.format(\"A [AArray=%s, aBool=%s, anA=%s, floatArray=%s, hashMap=%s, intArray=%s, integerCollection=%s, list=%s, object=%s, stringArray=%s, wildCollection=%s, charArrayMethod()=%s, floatArrayMethod()=%s]\", AArray != null ? \"[]\" : null, aBool, anA, floatArray != null ? \"[]\" : null, hashMap != null ? \"[]\" : null, intArray != null ? \"[]\" : null, integerCollection != null ? \"[]\" : null, list != null ? \"[]\" : null, object, stringArray != null ? \"[]\" : null, wildCollection != null ? \"[]\" : null, charArrayMethod() != null ? \"[]\" : null, floatArrayMethod() != null ? \"[]\" : null);\r\n"
				+ "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - custom array, limit of elements, 'use keyword this'
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatLimitThis() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.useKeywordThis= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 3;

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	Object object;\r\n"
				+ "	A anA;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n"
				+ "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n"
				+ "	}\r\n"
				+ "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n"
				+ "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		final int maxLen = 10;\r\n"
				+ "		return String.format(\"A [AArray=%s, aBool=%s, anA=%s, floatArray=%s, hashMap=%s, intArray=%s, integerCollection=%s, list=%s, object=%s, stringArray=%s, wildCollection=%s, charArrayMethod()=%s, floatArrayMethod()=%s]\", this.AArray, this.aBool, this.anA, this.floatArray, this.hashMap != null ? this.toString(this.hashMap.entrySet(), maxLen) : null, this.intArray, this.integerCollection != null ? this.toString(this.integerCollection, maxLen) : null, this.list != null ? this.toString(this.list, maxLen) : null, this.object, this.stringArray, this.wildCollection != null ? this.toString(this.wildCollection, maxLen) : null, this.charArrayMethod(), this.floatArrayMethod());\r\n"
				+ "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"[\");\r\n"
				+ "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0) {\r\n" + "				builder.append(\", \");\r\n"
				+ "			}\r\n" + "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - basic case
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilder() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					builder.append("aStringMethod()", aStringMethod());\r
					builder.append("aFloatMethod()", aFloatMethod());\r
					builder.append("anArrayMethod()", anArrayMethod());\r
					builder.append("aBool", aBool);\r
					builder.append("aString", aString);\r
					builder.append("anInt", anInt);\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					if (aStringMethod() != null) {\r
						builder.append("aStringMethod()", aStringMethod());\r
					}\r
					builder.append("aFloatMethod()", aFloatMethod());\r
					if (anArrayMethod() != null) {\r
						builder.append("anArrayMethod()", anArrayMethod());\r
					}\r
					builder.append("aBool", aBool);\r
					if (aString != null) {\r
						builder.append("aString", aString);\r
					}\r
					builder.append("anInt", anInt);\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - custom array toString without limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderArray() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n"
				+ "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] anArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n"
				+ "	java.util.List<Boolean> list;\r\n" + "\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "intArray", "list", "object", "stringArray", "anArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Arrays;\r\n" + "\r\n" + "import com.pack.ToStringBuilder;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n"
				+ "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n"
				+ "	char[] anArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	java.util.List<Boolean> list;\r\n" + "	@Override\r\n" + "	public String toString() {\r\n"
				+ "		ToStringBuilder builder = new ToStringBuilder(this);\r\n" + "		builder.append(\"AArray\", Arrays.toString(AArray));\r\n" + "		builder.append(\"aBool\", aBool);\r\n"
				+ "		builder.append(\"anA\", anA);\r\n" + "		builder.append(\"floatArray\", Arrays.toString(floatArray));\r\n" + "		builder.append(\"intArray\", Arrays.toString(intArray));\r\n"
				+ "		builder.append(\"list\", list);\r\n" + "		builder.append(\"object\", object);\r\n" + "		builder.append(\"stringArray\", Arrays.toString(stringArray));\r\n"
				+ "		builder.append(\"anArrayMethod()\", Arrays.toString(anArrayMethod()));\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - limit of elements but not arrays
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.Iterator;\r\n" + "import java.util.List;\r\n" + "\r\n"
				+ "import com.pack.ToStringBuilder;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n"
				+ "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n" + "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	@Override\r\n" + "	public String toString() {\r\n" + "		final int maxLen = 10;\r\n"
				+ "		ToStringBuilder builder = new ToStringBuilder(this);\r\n" + "		builder.append(\"AArray\", AArray);\r\n" + "		builder.append(\"aBool\", aBool);\r\n"
				+ "		builder.append(\"anA\", anA);\r\n" + "		builder.append(\"floatArray\", floatArray);\r\n"
				+ "		builder.append(\"hashMap\", hashMap != null ? toString(hashMap.entrySet(), maxLen) : null);\r\n" + "		builder.append(\"intArray\", intArray);\r\n"
				+ "		builder.append(\"integerCollection\", integerCollection != null ? toString(integerCollection, maxLen) : null);\r\n"
				+ "		builder.append(\"list\", list != null ? toString(list, maxLen) : null);\r\n" + "		builder.append(\"object\", object);\r\n" + "		builder.append(\"stringArray\", stringArray);\r\n"
				+ "		builder.append(\"wildCollection\", wildCollection != null ? toString(wildCollection, maxLen) : null);\r\n" + "		builder.append(\"charArrayMethod()\", charArrayMethod());\r\n"
				+ "		builder.append(\"floatArrayMethod()\", floatArrayMethod());\r\n" + "		return builder.toString();\r\n" + "	}\r\n"
				+ "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n" + "		builder.append(\"[\");\r\n" + "		int i = 0;\r\n"
				+ "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0) {\r\n" + "				builder.append(\", \");\r\n" + "			}\r\n"
				+ "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}


	/**
	 * Custom ToString() builder - custom array toString and limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderArrayLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Arrays;\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n" + "\r\n" + "import com.pack.ToStringBuilder;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n"
				+ "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n"
				+ "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n" + "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n" + "	Collection<Integer> integerCollection;\r\n" + "	@Override\r\n"
				+ "	public String toString() {\r\n" + "		final int maxLen = 10;\r\n" + "		ToStringBuilder builder = new ToStringBuilder(this);\r\n"
				+ "		builder.append(\"AArray\", AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) : null);\r\n" + "		builder.append(\"aBool\", aBool);\r\n"
				+ "		builder.append(\"anA\", anA);\r\n"
				+ "		builder.append(\"floatArray\", floatArray != null ? Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) : null);\r\n"
				+ "		builder.append(\"hashMap\", hashMap != null ? toString(hashMap.entrySet(), maxLen) : null);\r\n"
				+ "		builder.append(\"intArray\", intArray != null ? Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) : null);\r\n"
				+ "		builder.append(\"integerCollection\", integerCollection != null ? toString(integerCollection, maxLen) : null);\r\n"
				+ "		builder.append(\"list\", list != null ? toString(list, maxLen) : null);\r\n" + "		builder.append(\"object\", object);\r\n"
				+ "		builder.append(\"stringArray\", stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) : null);\r\n"
				+ "		builder.append(\"wildCollection\", wildCollection != null ? toString(wildCollection, maxLen) : null);\r\n"
				+ "		builder.append(\"charArrayMethod()\", charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) : null);\r\n"
				+ "		builder.append(\"floatArrayMethod()\", floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : null);\r\n"
				+ "		return builder.toString();\r\n" + "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n"
				+ "		builder.append(\"[\");\r\n" + "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0) {\r\n"
				+ "				builder.append(\", \");\r\n" + "			}\r\n" + "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n"
				+ "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - custom array toString and limit of elements, unique names needed
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderArrayLimitUnique() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	int[] intArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	List list;\r\n"
				+ "	HashMap hashMap;\r\n" + "	Collection wildCollection;\r\n" + "	Collection integerCollection;\r\n" + "	Object builder;\r\n" + "	Object buffer;\r\n" + "	Object maxLen;\r\n"
				+ "	Object len;\r\n" + "	Object collection;\r\n" + "	Object array;\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aBool", "intArray", "stringArray", "AArray", "list", "hashMap", "wildCollection", "integerCollection", "builder", "buffer",
				"maxLen", "len", "collection", "array" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Arrays;\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n" + "\r\n" + "import com.pack.ToStringBuilder;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n"
				+ "	int[] intArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	List list;\r\n" + "	HashMap hashMap;\r\n" + "	Collection wildCollection;\r\n"
				+ "	Collection integerCollection;\r\n" + "	Object builder;\r\n" + "	Object buffer;\r\n" + "	Object maxLen;\r\n" + "	Object len;\r\n" + "	Object collection;\r\n" + "	Object array;\r\n"
				+ "	@Override\r\n" + "	public String toString() {\r\n" + "		final int maxLen2 = 10;\r\n" + "		ToStringBuilder builder2 = new ToStringBuilder(this);\r\n"
				+ "		builder2.append(\"aBool\", aBool);\r\n"
				+ "		builder2.append(\"intArray\", intArray != null ? Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen2))) : null);\r\n"
				+ "		builder2.append(\"stringArray\", stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen2)) : null);\r\n"
				+ "		builder2.append(\"AArray\", AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen2)) : null);\r\n"
				+ "		builder2.append(\"list\", list != null ? toString(list, maxLen2) : null);\r\n"
				+ "		builder2.append(\"hashMap\", hashMap != null ? toString(hashMap.entrySet(), maxLen2) : null);\r\n"
				+ "		builder2.append(\"wildCollection\", wildCollection != null ? toString(wildCollection, maxLen2) : null);\r\n"
				+ "		builder2.append(\"integerCollection\", integerCollection != null ? toString(integerCollection, maxLen2) : null);\r\n" + "		builder2.append(\"builder\", builder);\r\n"
				+ "		builder2.append(\"buffer\", buffer);\r\n" + "		builder2.append(\"maxLen\", maxLen);\r\n" + "		builder2.append(\"len\", len);\r\n"
				+ "		builder2.append(\"collection\", collection);\r\n" + "		builder2.append(\"array\", array);\r\n" + "		return builder2.toString();\r\n" + "	}\r\n"
				+ "	private String toString(Collection<?> collection2, int maxLen2) {\r\n" + "		StringBuilder builder2 = new StringBuilder();\r\n" + "		builder2.append(\"[\");\r\n"
				+ "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection2.iterator(); iterator.hasNext() && i < maxLen2; i++) {\r\n" + "			if (i > 0) {\r\n"
				+ "				builder2.append(\", \");\r\n" + "			}\r\n" + "			builder2.append(iterator.next());\r\n" + "		}\r\n" + "		builder2.append(\"]\");\r\n" + "		return builder2.toString();\r\n"
				+ "	}\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - skip nulls, use keyword this, no one-line blocks
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderNullsThisNoBlocks() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.skipNulls= true;
		fSettings2.useBlocks= false;
		fSettings2.useKeywordThis= true;
		fSettings2.toStringStyle= 4;

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n" + "\r\n"
				+ "import com.pack.ToStringBuilder;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	Object object;\r\n" + "	A anA;\r\n"
				+ "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n"
				+ "	float[] floatArrayMethod() {\r\n" + "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	@Override\r\n" + "	public String toString() {\r\n" + "		ToStringBuilder builder = new ToStringBuilder(this);\r\n"
				+ "		if (this.AArray != null)\r\n" + "			builder.append(\"AArray\", this.AArray);\r\n" + "		builder.append(\"aBool\", this.aBool);\r\n" + "		if (this.anA != null)\r\n"
				+ "			builder.append(\"anA\", this.anA);\r\n" + "		if (this.floatArray != null)\r\n" + "			builder.append(\"floatArray\", this.floatArray);\r\n" + "		if (this.hashMap != null)\r\n"
				+ "			builder.append(\"hashMap\", this.hashMap);\r\n" + "		if (this.intArray != null)\r\n" + "			builder.append(\"intArray\", this.intArray);\r\n"
				+ "		if (this.integerCollection != null)\r\n" + "			builder.append(\"integerCollection\", this.integerCollection);\r\n" + "		if (this.list != null)\r\n"
				+ "			builder.append(\"list\", this.list);\r\n" + "		if (this.object != null)\r\n" + "			builder.append(\"object\", this.object);\r\n" + "		if (this.stringArray != null)\r\n"
				+ "			builder.append(\"stringArray\", this.stringArray);\r\n" + "		if (this.wildCollection != null)\r\n" + "			builder.append(\"wildCollection\", this.wildCollection);\r\n"
				+ "		if (this.charArrayMethod() != null)\r\n" + "			builder.append(\"charArrayMethod()\", this.charArrayMethod());\r\n" + "		if (this.floatArrayMethod() != null)\r\n"
				+ "			builder.append(\"floatArrayMethod()\", this.floatArrayMethod());\r\n" + "		return builder.toString();\r\n" + "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - custom array, limit elements, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderArrayLimitNulls() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + " int anInt;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n"
				+ "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n"
				+ "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n" + "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n"
				+ "	Collection<Integer> integerCollection;\r\n" + "	\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 4;

		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n" + "\r\n" + "import java.util.Arrays;\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.Iterator;\r\n"
				+ "import java.util.List;\r\n" + "\r\n" + "import com.pack.ToStringBuilder;\r\n" + "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n"
				+ " int anInt;\r\n" + "	Object object;\r\n" + "	A anA;\r\n" + "	int[] intArray;\r\n" + "	float[] floatArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n"
				+ "	char[] charArrayMethod() {\r\n" + "		return new char[0];\r\n" + "	}\r\n" + "	float[] floatArrayMethod() {\r\n" + "		return null;\r\n" + "	}\r\n" + "	List<Boolean> list;\r\n"
				+ "	HashMap<Integer, String> hashMap;\r\n" + "	Collection<?> wildCollection;\r\n" + "	Collection<Integer> integerCollection;\r\n" + "	@Override\r\n"
				+ "	public String toString() {\r\n" + "		final int maxLen = 10;\r\n" + "		ToStringBuilder builder = new ToStringBuilder(this);\r\n" + "		if (AArray != null) {\r\n"
				+ "			builder.append(\"AArray\", Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)));\r\n" + "		}\r\n" + "		builder.append(\"aBool\", aBool);\r\n"
				+ "		builder.append(\"anInt\", anInt);\r\n" + "		if (anA != null) {\r\n" + "			builder.append(\"anA\", anA);\r\n" + "		}\r\n" + "		if (floatArray != null) {\r\n"
				+ "			builder.append(\"floatArray\", Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))));\r\n" + "		}\r\n" + "		if (hashMap != null) {\r\n"
				+ "			builder.append(\"hashMap\", toString(hashMap.entrySet(), maxLen));\r\n" + "		}\r\n" + "		if (intArray != null) {\r\n"
				+ "			builder.append(\"intArray\", Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))));\r\n" + "		}\r\n" + "		if (integerCollection != null) {\r\n"
				+ "			builder.append(\"integerCollection\", toString(integerCollection, maxLen));\r\n" + "		}\r\n" + "		if (list != null) {\r\n"
				+ "			builder.append(\"list\", toString(list, maxLen));\r\n" + "		}\r\n" + "		if (object != null) {\r\n" + "			builder.append(\"object\", object);\r\n" + "		}\r\n"
				+ "		if (stringArray != null) {\r\n" + "			builder.append(\"stringArray\", Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)));\r\n" + "		}\r\n"
				+ "		if (wildCollection != null) {\r\n" + "			builder.append(\"wildCollection\", toString(wildCollection, maxLen));\r\n" + "		}\r\n" + "		if (charArrayMethod() != null) {\r\n"
				+ "			builder.append(\"charArrayMethod()\", Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))));\r\n" + "		}\r\n"
				+ "		if (floatArrayMethod() != null) {\r\n"
				+ "			builder.append(\"floatArrayMethod()\", Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))));\r\n" + "		}\r\n"
				+ "		return builder.toString();\r\n" + "	}\r\n" + "	private String toString(Collection<?> collection, int maxLen) {\r\n" + "		StringBuilder builder = new StringBuilder();\r\n"
				+ "		builder.append(\"[\");\r\n" + "		int i = 0;\r\n" + "		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r\n" + "			if (i > 0) {\r\n"
				+ "				builder.append(\", \");\r\n" + "			}\r\n" + "			builder.append(iterator.next());\r\n" + "		}\r\n" + "		builder.append(\"]\");\r\n" + "		return builder.toString();\r\n"
				+ "	}\r\n" + "	\r\n" + "}\r\n" + "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - chained calls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedCustomBuilder() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.chainCalls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					builder.append("aStringMethod()", aStringMethod()).append("aFloatMethod()", aFloatMethod()).append("anArrayMethod()", anArrayMethod()).append("aBool", aBool).append("aString", aString).append("anInt", anInt);\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - chained calls, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedCustomBuilderNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aString", "aBool", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.chainCalls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					if (aStringMethod() != null) {\r
						builder.append("aStringMethod()", aStringMethod());\r
					}\r
					builder.append("aFloatMethod()", aFloatMethod());\r
					if (anArrayMethod() != null) {\r
						builder.append("anArrayMethod()", anArrayMethod());\r
					}\r
					if (aString != null) {\r
						builder.append("aString", aString);\r
					}\r
					builder.append("aBool", aBool).append("anInt", anInt);\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - chained calls, add comment
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedCustomBuilderComments() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aString", "aBool", "anInt" });
		fSettings2.createComments= true;
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.chainCalls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					builder.append("aStringMethod()", aStringMethod()).append("aFloatMethod()", aFloatMethod()).append("anArrayMethod()", anArrayMethod()).append("aString", aString).append("aBool", aBool).append("anInt", anInt);\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom toString() builder - alternative class, basic case
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void alternativeCustomBuilder() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.className= "org.another.pack.AnotherToStringCreator";
		fSettings2.customBuilderSettings.variableName= "creator";
		fSettings2.customBuilderSettings.appendMethod= "addSth";
		fSettings2.customBuilderSettings.resultMethod= "getResult";
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import org.another.pack.AnotherToStringCreator;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					AnotherToStringCreator creator = new AnotherToStringCreator(this);\r
					creator.addSth(aStringMethod(), "aStringMethod()");\r
					creator.addSth(aFloatMethod(), "aFloatMethod()");\r
					creator.addSth(anArrayMethod(), "anArrayMethod()");\r
					creator.addSth(aBool, "aBool");\r
					creator.addSth(aString, "aString");\r
					creator.addSth("anInt", anInt);\r
					return creator.getResult();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom toString() builder - alternative class, unique names needed
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void alternativeCustomBuilderUnique() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + "\r\n" + "import java.util.Collection;\r\n" + "import java.util.HashMap;\r\n" + "import java.util.List;\r\n"
				+ "\r\n" + "public class A {\r\n" + "\r\n" + "	boolean aBool;\r\n" + "	int[] intArray;\r\n" + "	String[] stringArray;\r\n" + "	A[] AArray;\r\n" + "	List list;\r\n"
				+ "	HashMap hashMap;\r\n" + "	Collection wildCollection;\r\n" + "	Collection integerCollection;\r\n" + "	Object builder;\r\n" + "	Object buffer;\r\n" + "	Object maxLen;\r\n"
				+ "	Object len;\r\n" + "	Object collection;\r\n" + "	Object array;\r\n" + "	Object creator;\r\n" + "}\r\n" + "", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aBool", "intArray", "stringArray", "AArray", "list", "hashMap", "wildCollection", "integerCollection", "builder", "buffer",
				"maxLen", "len", "collection", "array", "creator" });
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.className= "org.another.pack.AnotherToStringCreator";
		fSettings2.customBuilderSettings.variableName= "creator";
		fSettings2.customBuilderSettings.appendMethod= "addSth";
		fSettings2.customBuilderSettings.resultMethod= "getResult";
		runOperation(a.getType("A"), members, null);

		String expected= "package p;\r\n"
				+ "\r\n"
				+ "import java.util.Collection;\r\n"
				+ "import java.util.HashMap;\r\n"
				+ "import java.util.List;\r\n"
				+ "\r\n"
				+ "import org.another.pack.AnotherToStringCreator;\r\n"
				+ "\r\n"
				+ "public class A {\r\n"
				+ "\r\n"
				+ "	boolean aBool;\r\n"
				+ "	int[] intArray;\r\n"
				+ "	String[] stringArray;\r\n"
				+ "	A[] AArray;\r\n"
				+ "	List list;\r\n"
				+ "	HashMap hashMap;\r\n"
				+ "	Collection wildCollection;\r\n"
				+ "	Collection integerCollection;\r\n"
				+ "	Object builder;\r\n"
				+ "	Object buffer;\r\n"
				+ "	Object maxLen;\r\n"
				+ "	Object len;\r\n"
				+ "	Object collection;\r\n"
				+ "	Object array;\r\n"
				+ "	Object creator;\r\n"
				+ "	@Override\r\n"
				+ "	public String toString() {\r\n"
				+ "		AnotherToStringCreator creator2 = new AnotherToStringCreator(this);\r\n"
				+ "		creator2.addSth(aBool, \"aBool\");\r\n"
				+ "		creator2.addSth(intArray, \"intArray\");\r\n"
				+ "		creator2.addSth(stringArray, \"stringArray\");\r\n"
				+ "		creator2.addSth(AArray, \"AArray\");\r\n"
				+ "		creator2.addSth(list, \"list\");\r\n"
				+ "		creator2.addSth(hashMap, \"hashMap\");\r\n"
				+ "		creator2.addSth(wildCollection, \"wildCollection\");\r\n"
				+ "		creator2.addSth(integerCollection, \"integerCollection\");\r\n"
				+ "		creator2.addSth(builder, \"builder\");\r\n"
				+ "		creator2.addSth(buffer, \"buffer\");\r\n"
				+ "		creator2.addSth(maxLen, \"maxLen\");\r\n"
				+ "		creator2.addSth(len, \"len\");\r\n"
				+ "		creator2.addSth(collection, \"collection\");\r\n"
				+ "		creator2.addSth(array, \"array\");\r\n"
				+ "		creator2.addSth(creator, \"creator\");\r\n"
				+ "		return creator2.getResult();\r\n"
				+ "	}\r\n"
				+ "}\r\n"
				+ "";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom toString() builder - alternative class, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void alternativeCustomBuilderNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aString", "aBool", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.className= "org.another.pack.AnotherToStringCreator";
		fSettings2.customBuilderSettings.variableName= "creator";
		fSettings2.customBuilderSettings.appendMethod= "addSth";
		fSettings2.customBuilderSettings.resultMethod= "getResult";
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import org.another.pack.AnotherToStringCreator;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					AnotherToStringCreator creator = new AnotherToStringCreator(this);\r
					if (aStringMethod() != null) {\r
						creator.addSth(aStringMethod(), "aStringMethod()");\r
					}\r
					creator.addSth(aFloatMethod(), "aFloatMethod()");\r
					if (anArrayMethod() != null) {\r
						creator.addSth(anArrayMethod(), "anArrayMethod()");\r
					}\r
					if (aString != null) {\r
						creator.addSth(aString, "aString");\r
					}\r
					creator.addSth(aBool, "aBool");\r
					creator.addSth("anInt", anInt);\r
					return creator.getResult();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom toString() builder - alternative class, chained calls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedAlternativeCustomBuilderCreator() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.className= "org.another.pack.AnotherToStringCreator";
		fSettings2.customBuilderSettings.variableName= "creator";
		fSettings2.customBuilderSettings.appendMethod= "addSth";
		fSettings2.customBuilderSettings.resultMethod= "getResult";
		fSettings2.customBuilderSettings.chainCalls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import org.another.pack.AnotherToStringCreator;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					AnotherToStringCreator creator = new AnotherToStringCreator(this);\r
					creator.addSth(aStringMethod(), "aStringMethod()").addSth(aFloatMethod(), "aFloatMethod()").addSth(anArrayMethod(), "anArrayMethod()").addSth(aBool, "aBool");\r
					creator.addSth(aString, "aString").addSth("anInt", anInt);\r
					return creator.getResult();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom toString() builder - alternative class, chained calls, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedAlternativeCustomBuilderNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "anArrayMethod", "aString", "aFloatMethod", "aBool", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.className= "org.another.pack.AnotherToStringCreator";
		fSettings2.customBuilderSettings.variableName= "creator";
		fSettings2.customBuilderSettings.appendMethod= "addSth";
		fSettings2.customBuilderSettings.resultMethod= "getResult";
		fSettings2.customBuilderSettings.chainCalls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import org.another.pack.AnotherToStringCreator;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					AnotherToStringCreator creator = new AnotherToStringCreator(this);\r
					if (aStringMethod() != null) {\r
						creator.addSth(aStringMethod(), "aStringMethod()");\r
					}\r
					if (anArrayMethod() != null) {\r
						creator.addSth(anArrayMethod(), "anArrayMethod()");\r
					}\r
					if (aString != null) {\r
						creator.addSth(aString, "aString");\r
					}\r
					creator.addSth(aFloatMethod(), "aFloatMethod()").addSth(aBool, "aBool");\r
					creator.addSth("anInt", anInt);\r
					return creator.getResult();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom toString() builder - alternative class with append method that takes only one argument
	 * for most of the types
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedOneArgumentCustomBuilders() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IPackageFragment packageFragment= fRoot.createPackageFragment("com.simple.pack", true, null);
		ICompilationUnit compilationUnit= packageFragment.getCompilationUnit("ToStringBuilder.java");
		compilationUnit
				.createType(
						"package com.simple.pack;\npublic class ToStringBuilder {\npublic ToStringBuilder(Object o){\n}\npublic ToStringBuilder append(Object o){\nreturn null;\n}\npublic ToStringBuilder append(String s1, String s2) {\nreturn null;\n}\npublic String toString(){\nreturn null;\n}\n}\n",
						null, true, null);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "anArrayMethod", "aString", "aFloatMethod", "aBool", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.customBuilderSettings.className= "com.simple.pack.ToStringBuilder";
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import com.simple.pack.ToStringBuilder;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					if (aStringMethod() != null) {\r
						builder.append("aStringMethod()", aStringMethod());\r
					}\r
					if (anArrayMethod() != null) {\r
						builder.append(anArrayMethod());\r
					}\r
					if (aString != null) {\r
						builder.append("aString", aString);\r
					}\r
					builder.append(aFloatMethod());\r
					builder.append(aBool);\r
					builder.append(anInt);\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}
}
