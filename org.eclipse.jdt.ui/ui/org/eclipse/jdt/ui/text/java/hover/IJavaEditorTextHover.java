/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.text.java.hover;

import org.eclipse.jface.text.ITextHover;

import org.eclipse.ui.IEditorPart;

/**
 * Provides a hover popup which appears on top of an editor with relevant
 * display information. If the text hover does not provide information no
 * hover popup is shown.<p>
 * Clients may implement this interface.
 *
 * @see IEditorPart
 * @see ITextHover
 * 
 * @since 2.0
 */
public interface IJavaEditorTextHover extends ITextHover {

	/**
	 * Sets the editor for which the hover is shown.
	 * 
	 * @param editor the editor on which the hover popup should be shown
	 */
	void setEditor(IEditorPart editor);

}

