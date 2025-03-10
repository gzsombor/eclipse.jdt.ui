/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
 *     Red Hat Inc.- refactored to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;


public class ControlStatementsFix extends CompilationUnitRewriteOperationsFixCore {

	private final static class ControlStatementFinder extends GenericVisitor {

		private final List<CompilationUnitRewriteOperationWithSourceRange> fResult;
		private final boolean fFindControlStatementsWithoutBlock;
		private final boolean fRemoveUnnecessaryBlocks;
		private final boolean fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow;

		public ControlStatementFinder(boolean findControlStatementsWithoutBlock,
				boolean removeUnnecessaryBlocks,
				boolean removeUnnecessaryBlocksOnlyWhenReturnOrThrow,
				List<CompilationUnitRewriteOperationWithSourceRange> resultingCollection) {

			fFindControlStatementsWithoutBlock= findControlStatementsWithoutBlock;
			fRemoveUnnecessaryBlocks= removeUnnecessaryBlocks;
			fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow= removeUnnecessaryBlocksOnlyWhenReturnOrThrow;
			fResult= resultingCollection;
		}

		@Override
		public boolean visit(DoStatement node) {
			handle(node.getBody(), DoStatement.BODY_PROPERTY);

			return super.visit(node);
		}

		@Override
		public boolean visit(ForStatement node) {
			handle(node.getBody(), ForStatement.BODY_PROPERTY);

			return super.visit(node);
		}

		@Override
		public boolean visit(EnhancedForStatement node) {
			handle(node.getBody(), EnhancedForStatement.BODY_PROPERTY);

			return super.visit(node);
		}

		@Override
		public boolean visit(IfStatement statement) {
			handle(statement.getThenStatement(), IfStatement.THEN_STATEMENT_PROPERTY);

			Statement elseStatement= statement.getElseStatement();
			if (elseStatement != null && !(elseStatement instanceof IfStatement)) {
				handle(elseStatement, IfStatement.ELSE_STATEMENT_PROPERTY);
			}

			return super.visit(statement);
		}

		@Override
		public boolean visit(WhileStatement node) {
			handle(node.getBody(), WhileStatement.BODY_PROPERTY);

			return super.visit(node);
		}

		private void handle(Statement body, ChildPropertyDescriptor bodyProperty) {
			if ((body.getFlags() & ASTNode.RECOVERED) != 0)
				return;
			Statement parent= (Statement)body.getParent();
			if ((parent.getFlags() & ASTNode.RECOVERED) != 0)
				return;

			if (fRemoveUnnecessaryBlocksOnlyWhenReturnOrThrow) {
				if (!(body instanceof Block)) {
					if (body.getNodeType() != ASTNode.IF_STATEMENT && body.getNodeType() != ASTNode.RETURN_STATEMENT && body.getNodeType() != ASTNode.THROW_STATEMENT) {
						fResult.add(new AddBlockOperation(bodyProperty, body, parent));
					}
				} else {
					if (RemoveBlockOperation.satisfiesCleanUpPrecondition(parent, bodyProperty, true)) {
						fResult.add(new RemoveBlockOperation(parent, bodyProperty));
					}
				}
			} else if (fFindControlStatementsWithoutBlock) {
				if (!(body instanceof Block)) {
					fResult.add(new AddBlockOperation(bodyProperty, body, parent));
				}
			} else if (fRemoveUnnecessaryBlocks) {
				if (RemoveBlockOperation.satisfiesCleanUpPrecondition(parent, bodyProperty, false)) {
					fResult.add(new RemoveBlockOperation(parent, bodyProperty));
				}
			}
		}

	}

	private static class IfElseIterator {

		private IfStatement fCursor;

		public IfElseIterator(IfStatement item) {
			fCursor= findStart(item);
		}

		public IfStatement next() {
			if (!hasNext())
				return null;

			IfStatement result= fCursor;

			if (fCursor.getElseStatement() instanceof IfStatement) {
				fCursor= (IfStatement)fCursor.getElseStatement();
			} else {
				fCursor= null;
			}

			return result;
		}

		public boolean hasNext() {
			return fCursor != null;
		}

		private IfStatement findStart(IfStatement item) {
            while (item.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
            	item= (IfStatement)item.getParent();
            }
            return item;
        }
	}

	private static final class AddBlockOperation extends CompilationUnitRewriteOperationWithSourceRange {

		private final ChildPropertyDescriptor fBodyProperty;
		private final Statement fBody;
		private final Statement fControlStatement;

		public AddBlockOperation(ChildPropertyDescriptor bodyProperty, Statement body, Statement controlStatement) {
			fBodyProperty= bodyProperty;
			fBody= body;
			fControlStatement= controlStatement;
		}

		@Override
		public void rewriteASTInternal(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			String label;
			ASTNode expression= null;
			int statementType= -1;
			int defaultStartPosition= fControlStatement.getStartPosition();
			if (fBodyProperty == IfStatement.THEN_STATEMENT_PROPERTY) {
				label = FixMessages.CodeStyleFix_ChangeIfToBlock_desription;
				expression= ((IfStatement)fControlStatement).getExpression();
				if (((IfStatement)fControlStatement).getElseStatement() == null) {
					statementType= ASTNode.IF_STATEMENT;
				}
			} else if (fBodyProperty == IfStatement.ELSE_STATEMENT_PROPERTY) {
				label = FixMessages.CodeStyleFix_ChangeElseToBlock_description;
				Statement thenStatement= ((IfStatement)fControlStatement).getThenStatement();
				defaultStartPosition= thenStatement.getStartPosition() + thenStatement.getLength();
			} else {
				label = FixMessages.CodeStyleFix_ChangeControlToBlock_description;
				if (fBodyProperty == WhileStatement.BODY_PROPERTY) {
					expression= ((WhileStatement)fControlStatement).getExpression();
					statementType= ASTNode.WHILE_STATEMENT;
				} else if (fBodyProperty == ForStatement.BODY_PROPERTY) {
					expression= ((ForStatement)fControlStatement).getExpression();
					statementType= ASTNode.FOR_STATEMENT;
				} else if (fBodyProperty == EnhancedForStatement.BODY_PROPERTY) {
					expression= ((EnhancedForStatement)fControlStatement).getExpression();
					statementType= ASTNode.ENHANCED_FOR_STATEMENT;
				}
			}

			TextEditGroup group= createTextEditGroup(label, cuRewrite);
			List<Comment> commentsToPreserve= new ArrayList<>();
			CompilationUnit cuRoot= cuRewrite.getRoot();
			int controlStatementLine= cuRoot.getLineNumber(fControlStatement.getStartPosition());
			int bodyLine= cuRoot.getLineNumber(fBody.getStartPosition());
			// If single body statement is on next line, we need to preserve any comments pertaining to the
			// control statement (e.g. NLS comment for if expression)
			if (controlStatementLine != bodyLine) {
				int startPosition= expression == null ? defaultStartPosition : (expression.getStartPosition() + cuRoot.getExtendedLength(expression));
				List<Comment> comments= cuRoot.getCommentList();
				for (Comment comment : comments) {
					int commentLine= cuRoot.getLineNumber(comment.getStartPosition());
					if (commentLine == controlStatementLine && comment.getStartPosition() > startPosition &&
							comment.getStartPosition() < fBody.getStartPosition()) {
						commentsToPreserve.add(comment);
					}
				}
			}
			String blockString= "{"; //$NON-NLS-1$
			IBuffer cuBuffer= cuRewrite.getCu().getBuffer();
			Block replacingBody= null;
			String blockPosition= JavaCore.getOption(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK);
			boolean blockEndOfLine= blockPosition.equals(DefaultCodeFormatterConstants.END_OF_LINE);
			if (!commentsToPreserve.isEmpty()) {
				// Get extended body text and convert multiple indent tabs to be just one tab as they will be relative to control statement
				String bodyString= cuBuffer.getText(cuRoot.getExtendedStartPosition(fBody), cuRoot.getExtendedLength(fBody))
						.replaceAll("\\r\\n|\\r|\\n(\\t|\\s)*", System.lineSeparator() + "\t"); //$NON-NLS-1$ //$NON-NLS-2$
				if (blockEndOfLine || statementType == -1) {
					// To ensure the comments to preserve end up on same line as the control statement, we need
					// to build the block manually as a string and then create a Block placeholder from it
					for (Comment comment : commentsToPreserve) {
						String commentString= cuBuffer.getText(comment.getStartPosition(), comment.getLength());
						blockString += " " + commentString; //$NON-NLS-1$
					}
					blockString += System.lineSeparator() + "\t" + bodyString + System.lineSeparator() + "}"; //$NON-NLS-1$ //$NON-NLS-2$
					replacingBody= (Block)rewrite.createStringPlaceholder(blockString, ASTNode.BLOCK);
				} else {
					Comment lastComment= commentsToPreserve.get(commentsToPreserve.size()-1);
					String newControlStatement= cuBuffer.getText(cuRoot.getExtendedStartPosition(fControlStatement),
							lastComment.getStartPosition() + lastComment.getLength() - cuRoot.getExtendedStartPosition(fControlStatement));
					newControlStatement += System.lineSeparator() + "{" + System.lineSeparator(); //$NON-NLS-1$
					newControlStatement += "\t" + bodyString + System.lineSeparator() + "}"; //$NON-NLS-1$ //$NON-NLS-2$
					Statement newStatement= (Statement)rewrite.createStringPlaceholder(newControlStatement, statementType);
					rewrite.replace(fControlStatement, newStatement, group);
					return;
				}
			} else {
				ASTNode moveTarget= rewrite.createMoveTarget(fBody);
				replacingBody= cuRewrite.getRoot().getAST().newBlock();
				replacingBody.statements().add(moveTarget);
			}
			rewrite.set(fControlStatement, fBodyProperty, replacingBody, group);
		}

	}

	static class RemoveBlockOperation extends CompilationUnitRewriteOperationWithSourceRange {

		private final Statement fStatement;
		private final ChildPropertyDescriptor fChild;

		public RemoveBlockOperation(Statement controlStatement, ChildPropertyDescriptor child) {
			fStatement= controlStatement;
			fChild= child;
		}

		@Override
		public void rewriteASTInternal(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore model) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();

			Block block= (Block)fStatement.getStructuralProperty(fChild);
			Statement statement= (Statement)block.statements().get(0);
			Statement moveTarget= (Statement)rewrite.createMoveTarget(statement);

			TextEditGroup group= createTextEditGroup(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, cuRewrite);
			rewrite.set(fStatement, fChild, moveTarget, group);
		}

		public static boolean satisfiesCleanUpPrecondition(Statement controlStatement, ChildPropertyDescriptor childDescriptor, boolean onlyReturnAndThrows) {
			return satisfiesPrecondition(controlStatement, childDescriptor, onlyReturnAndThrows, true);
		}

		public static boolean satisfiesQuickAssistPrecondition(Statement controlStatement, ChildPropertyDescriptor childDescriptor) {
			return satisfiesPrecondition(controlStatement, childDescriptor, false, false);
		}

		//Can the block around child with childDescriptor of controlStatement be removed?
        private static boolean satisfiesPrecondition(Statement controlStatement, ChildPropertyDescriptor childDescriptor, boolean onlyReturnAndThrows, boolean cleanUpCheck) {
        	Object child= controlStatement.getStructuralProperty(childDescriptor);

        	if (!(child instanceof Block))
        		return false;

        	Block block= (Block)child;
        	List<Statement> list= block.statements();
        	if (list.size() != 1)
        		return false;

        	ASTNode singleStatement= list.get(0);

        	if (onlyReturnAndThrows)
        		if (!(singleStatement instanceof ReturnStatement) && !(singleStatement instanceof ThrowStatement))
        			return false;

        	if (controlStatement instanceof IfStatement) {
        		// if (true) {
        		//  while (true)
        		// 	 if (false)
        		//    ;
        		// } else
        		//   ;

        		if (((IfStatement)controlStatement).getThenStatement() != child)
        			return true;//can always remove blocks in else part

        		IfStatement ifStatement= (IfStatement)controlStatement;
        		if (ifStatement.getElseStatement() == null)
        			return true;//can always remove if no else part

        		return !hasUnblockedIf((Statement)singleStatement, onlyReturnAndThrows, cleanUpCheck);
        	} else {
        		//if (true)
        		// while (true) {
        		//  if (false)
        		//   ;
        		// }
        		//else
        		// ;
        		if (!hasUnblockedIf((Statement)singleStatement, onlyReturnAndThrows, cleanUpCheck))
        			return true;

        		ASTNode currentChild= controlStatement;
        		ASTNode parent= currentChild.getParent();
        		while (true) {
        			Statement body= null;
        			if (parent instanceof IfStatement) {
        				body= ((IfStatement)parent).getThenStatement();
        				if (body == currentChild && ((IfStatement)parent).getElseStatement() != null)//->currentChild is an unblocked then part
        					return false;
        			} else if (parent instanceof WhileStatement) {
        				body= ((WhileStatement)parent).getBody();
        			} else if (parent instanceof DoStatement) {
        				body= ((DoStatement)parent).getBody();
        			} else if (parent instanceof ForStatement) {
        				body= ((ForStatement)parent).getBody();
        			} else if (parent instanceof EnhancedForStatement) {
        				body= ((EnhancedForStatement)parent).getBody();
        			} else {
        				return true;
        			}
        			if (body != currentChild)//->parents child is a block
        				return true;

        			currentChild= parent;
        			parent= currentChild.getParent();
        		}
        	}
        }

		private static boolean hasUnblockedIf(Statement p, boolean onlyReturnAndThrows, boolean cleanUpCheck) {
	        while (true) {
	        	if (p instanceof IfStatement) {
	        		return true;
	        	} else {

	        		ChildPropertyDescriptor childD= null;
	        		if (p instanceof WhileStatement) {
	        			childD= WhileStatement.BODY_PROPERTY;
	        		} else if (p instanceof ForStatement) {
	        			childD= ForStatement.BODY_PROPERTY;
	        		} else if (p instanceof EnhancedForStatement) {
	        			childD= EnhancedForStatement.BODY_PROPERTY;
	        		} else if (p instanceof DoStatement) {
	        			childD= DoStatement.BODY_PROPERTY;
	        		} else {
	        			return false;
	        		}
	        		Statement body= (Statement)p.getStructuralProperty(childD);
	        		if (body instanceof Block) {
	        			if (!cleanUpCheck) {
	        				return false;
	        			} else {
	        				if (!satisfiesPrecondition(p, childD, onlyReturnAndThrows, cleanUpCheck))
	        					return false;

	        				p= (Statement)((Block)body).statements().get(0);
	        			}
	        		} else {
	        			p= body;
	        		}
	        	}
	        }
        }

	}

	public static ControlStatementsFix[] createRemoveBlockFix(CompilationUnit compilationUnit, ASTNode node) {
		if (!(node instanceof Statement)) {
			return null;
		}
		Statement statement= (Statement) node;

		if (statement instanceof Block) {
			Block block= (Block)statement;
			if (block.statements().size() != 1)
				return null;

			ASTNode parent= block.getParent();
			if (!(parent instanceof Statement))
				return null;

			statement= (Statement)parent;
		}

		if (statement instanceof IfStatement) {
			List<ControlStatementsFix> result= new ArrayList<>();

			List<RemoveBlockOperation> removeAllList= new ArrayList<>();

			IfElseIterator iter= new IfElseIterator((IfStatement)statement);
			IfStatement item= null;
			while (iter.hasNext()) {
				item= iter.next();
				if (RemoveBlockOperation.satisfiesQuickAssistPrecondition(item, IfStatement.THEN_STATEMENT_PROPERTY)) {
            		RemoveBlockOperation op= new RemoveBlockOperation(item, IfStatement.THEN_STATEMENT_PROPERTY);
					removeAllList.add(op);
					if (item == statement)
						result.add(new ControlStatementsFix(FixMessages.ControlStatementsFix_removeIfBlock_proposalDescription, compilationUnit, new CompilationUnitRewriteOperationWithSourceRange[] {op}));
            	}
			}

			if (RemoveBlockOperation.satisfiesQuickAssistPrecondition(item, IfStatement.ELSE_STATEMENT_PROPERTY)) {
            	RemoveBlockOperation op= new RemoveBlockOperation(item, IfStatement.ELSE_STATEMENT_PROPERTY);
				removeAllList.add(op);
				if (item == statement)
					result.add(new ControlStatementsFix(FixMessages.ControlStatementsFix_removeElseBlock_proposalDescription, compilationUnit, new CompilationUnitRewriteOperationWithSourceRange[] {op}));
            }

			if (removeAllList.size() > 1) {
				CompilationUnitRewriteOperationWithSourceRange[] allConvert= removeAllList.toArray(new CompilationUnitRewriteOperationWithSourceRange[removeAllList.size()]);
				result.add(new ControlStatementsFix(FixMessages.ControlStatementsFix_removeIfElseBlock_proposalDescription, compilationUnit, allConvert));
            }

            return result.toArray(new ControlStatementsFix[result.size()]);
		} else if (statement instanceof WhileStatement) {
			if (RemoveBlockOperation.satisfiesQuickAssistPrecondition(statement, WhileStatement.BODY_PROPERTY)) {
				RemoveBlockOperation op= new RemoveBlockOperation(statement, WhileStatement.BODY_PROPERTY);
				return new ControlStatementsFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new CompilationUnitRewriteOperationWithSourceRange[] {op})};
			}
		} else if (statement instanceof ForStatement) {
			if (RemoveBlockOperation.satisfiesQuickAssistPrecondition(statement, ForStatement.BODY_PROPERTY)) {
				RemoveBlockOperation op= new RemoveBlockOperation(statement, ForStatement.BODY_PROPERTY);
				return new ControlStatementsFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new CompilationUnitRewriteOperationWithSourceRange[] {op})};
			}
		} else if (statement instanceof EnhancedForStatement) {
			if (RemoveBlockOperation.satisfiesQuickAssistPrecondition(statement, EnhancedForStatement.BODY_PROPERTY)) {
				RemoveBlockOperation op= new RemoveBlockOperation(statement, EnhancedForStatement.BODY_PROPERTY);
				return new ControlStatementsFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new CompilationUnitRewriteOperationWithSourceRange[] {op})};
			}
		} else if (statement instanceof DoStatement) {
			if (RemoveBlockOperation.satisfiesQuickAssistPrecondition(statement, DoStatement.BODY_PROPERTY)) {
				RemoveBlockOperation op= new RemoveBlockOperation(statement, DoStatement.BODY_PROPERTY);
				return new ControlStatementsFix[] {new ControlStatementsFix(FixMessages.ControlStatementsFix_removeBrackets_proposalDescription, compilationUnit, new CompilationUnitRewriteOperationWithSourceRange[] {op})};
			}
		}

		return null;
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit,
			boolean convertSingleStatementToBlock,
			boolean removeUnnecessaryBlock,
			boolean removeUnnecessaryBlockContainingReturnOrThrow) {

		if (!convertSingleStatementToBlock && !removeUnnecessaryBlock && !removeUnnecessaryBlockContainingReturnOrThrow)
			return null;

		List<CompilationUnitRewriteOperationWithSourceRange> operations= new ArrayList<>();
		ControlStatementFinder finder= new ControlStatementFinder(convertSingleStatementToBlock, removeUnnecessaryBlock, removeUnnecessaryBlockContainingReturnOrThrow, operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty())
			return null;

		CompilationUnitRewriteOperationWithSourceRange[] ops= operations.toArray(new CompilationUnitRewriteOperationWithSourceRange[operations.size()]);
		return new ControlStatementsFix(FixMessages.ControlStatementsFix_change_name, compilationUnit, ops);
	}

	protected ControlStatementsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationWithSourceRange[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
