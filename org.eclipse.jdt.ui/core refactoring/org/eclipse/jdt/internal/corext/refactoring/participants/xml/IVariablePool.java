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
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import org.eclipse.core.runtime.CoreException;

/**
 * A variable pool is used to manage a set of objects needed during
 * XML expression evaluation. A pool has a parent pool, can manage
 * a set of named variables and had a default variable. Default variable 
 * is used during XML expression evaluation if no explicit variable is 
 * referenced.
 * <p>
 * This interface is not intended to be implemented by clients. Clients
 * are allowed to instantiate <code>VariablePool</code> and <code>
 * DefaultVariable</code>
 * </p>
 * 
 * @since 3.0
 */
public interface IVariablePool {

	/**
	 * Returns the parent pool or <code>null</code> if no
	 * this is the root of the pool hierarchy.
	 * 
	 * @return the parent variable pool or <code>null</code>
	 */
	public IVariablePool getParent();
	
	/**
	 * Returns the root variable pool.
	 * 
	 * @return the root variable pool.
	 */
	public IVariablePool getRoot();
	
	/**
	 * Returns the default variable.
	 * 
	 * @return the default variable or <code>null</code> if
	 *  no default variable is managed.
	 */
	public Object getDefaultVariable();
	
	/**
	 * Adds a new named variable to this pool. If a variable
	 * with the name already exists the new one overrides the
	 * existing one.
	 * 
	 * @param name the variable's name
	 * @param value the variable's value
	 */
	public void addVariable(String name, Object value);
	
	/**
	 * Removes the variable managed under the given name
	 * from this pool.
	 * 
	 * @param name the variable's name
	 * @return the currently stored value or <code>null</code> if
	 *  the variable doesn't exist
	 */
	public Object removeVariable(String name);
	
	/**
	 * Returns the variable managed under the given name.
	 * 
	 * @param name the variable's name
	 * @return the variable's value
	 */
	public Object getVariable(String name);
	
	/**
	 * Resolves a variable for the given name and arguments. This
	 * method can be used to dynamically resolve variable such as
	 * plugin descriptors, resources, etc.
	 * 
	 * @param name the variable to resolve
	 * @param args an object array of arguments used to resolve the
	 *  variable
	 * @return the variable's value 
	 * @exception CoreException if an errors occurs while resolving
	 *  the variable
	 */
	public Object resolveVariable(String name, Object[] args) throws CoreException;
}
