/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;


import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.javaeditor.EditorHighlightingSynchronizer;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * A template proposal.
 */
public class LinkedNamesAssistProposal implements IJavaCompletionProposal, ICompletionProposalExtension2 {

	private SimpleName fNode;
	private Point fSelectedRegion; // initialized by apply()
	private ICompilationUnit fCompilationUnit;
	private String fLabel;
	private String fValueSuggestion;
			
	public LinkedNamesAssistProposal(ICompilationUnit cu, SimpleName node) {
		this(CorrectionMessages.getString("LinkedNamesAssistProposal.description"), cu, node, null); //$NON-NLS-1$
		fNode= node;
		fCompilationUnit= cu;
	}
	
	public LinkedNamesAssistProposal(String label, ICompilationUnit cu, SimpleName node, String valueSuggestion) {
		fLabel= label;
		fNode= node;
		fCompilationUnit= cu;
		fValueSuggestion= valueSuggestion;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#apply(org.eclipse.jface.text.ITextViewer, char, int, int)
	 */
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		try {
			fSelectedRegion= viewer.getSelectedRange();
			
			// get full ast
			CompilationUnit root= JavaPlugin.getDefault().getASTProvider().getAST(fCompilationUnit, ASTProvider.WAIT_YES, null);

			ASTNode nameNode= NodeFinder.perform(root, fNode.getStartPosition(), fNode.getLength());
			final int pos= fNode.getStartPosition();
			
			ASTNode[] sameNodes;
			if (nameNode instanceof SimpleName) {
				sameNodes= LinkedNodeFinder.findByNode(root, (SimpleName) nameNode);
			} else {
				sameNodes= new ASTNode[] { nameNode };
			}

			// sort for iteration order, starting with the node @ offset
			Arrays.sort(sameNodes, new Comparator() {
				
				public int compare(Object o1, Object o2) {
					return rank((ASTNode) o1) - rank((ASTNode) o2);
				}

				/**
				 * Returns the absolute rank of an <code>ASTNode</code>. Nodes 
				 * preceding <code>offset</code> are ranked last.
				 * 
				 * @param node the node to compute the rank for
				 * @return the rank of the node with respect to the invocation offset
				 */
				private int rank(ASTNode node) {
					int relativeRank= node.getStartPosition() + node.getLength() - pos;
					if (relativeRank < 0)
						return Integer.MAX_VALUE + relativeRank;
					else
						return relativeRank;
				}
				
			});
			
			IDocument document= viewer.getDocument();
			LinkedPositionGroup group= new LinkedPositionGroup();
			for (int i= 0; i < sameNodes.length; i++) {
				ASTNode elem= sameNodes[i];
				group.addPosition(new LinkedPosition(document, elem.getStartPosition(), elem.getLength(), i));
			}
			
			LinkedModeModel model= new LinkedModeModel();
			model.addGroup(group);
			model.forceInstall();
			JavaEditor editor= getJavaEditor();
			if (editor != null) {
				model.addLinkingListener(new EditorHighlightingSynchronizer(editor));
			}
			
			LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
//			ui.setInitialOffset(offset);
			ui.setExitPosition(viewer, offset, 0, LinkedPositionGroup.NO_STOP);
			ui.enter();
			

			if (fValueSuggestion != null) {
				IRegion selectedRegion= ui.getSelectedRegion();
				if (selectedRegion != null) {
					document.replace(selectedRegion.getOffset(), selectedRegion.getLength(), fValueSuggestion);
					selectedRegion= ui.getSelectedRegion();
					fSelectedRegion= new Point(selectedRegion.getOffset(), fValueSuggestion.length());
				}
			}
			
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}	

	/**
	 * Returns the currently active java editor, or <code>null</code> if it 
	 * cannot be determined.
	 * 
	 * @return  the currently active java editor, or <code>null</code>
	 */
	private JavaEditor getJavaEditor() {
		IEditorPart part= JavaPlugin.getActivePage().getActiveEditor();
		if (part instanceof JavaEditor)
			return (JavaEditor) part;
		else
			return null;
	}

	/*
	 * @see ICompletionProposal#apply(IDocument)
	 */
	public void apply(IDocument document) {
		// can't do anything
	}

	/*
	 * @see ICompletionProposal#getSelection(IDocument)
	 */
	public Point getSelection(IDocument document) {
		return fSelectedRegion;
	}

	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return CorrectionMessages.getString("LinkedNamesAssistProposal.proposalinfo"); //$NON-NLS-1$
	}

	/*
	 * @see ICompletionProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return fLabel;
	}

	/*
	 * @see ICompletionProposal#getImage()
	 */
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
	}

	/*
	 * @see ICompletionProposal#getContextInformation()
	 */
	public IContextInformation getContextInformation() {
		return null;
	}

	/*
	 * @see IJavaCompletionProposal#getRelevance()
	 */
	public int getRelevance() {
		return 1;
	}
		
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(org.eclipse.jface.text.ITextViewer, boolean)
	 */
	public void selected(ITextViewer textViewer, boolean smartToggle) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#unselected(org.eclipse.jface.text.ITextViewer)
	 */
	public void unselected(ITextViewer textViewer) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
	 */
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		return false;
	}

}
