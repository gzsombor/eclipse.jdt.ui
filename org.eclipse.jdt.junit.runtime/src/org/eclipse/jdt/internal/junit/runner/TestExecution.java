/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.runner;

import java.util.ArrayList;

public final class TestExecution {
	private boolean fShouldStop = false;

	private final IListensToTestExecutions fExecutionListener;

	private final IClassifiesThrowables fClassifier;

	private final ArrayList<IStopListener> fStopListeners = new ArrayList<>();

	public TestExecution(IListensToTestExecutions listener,
			IClassifiesThrowables classifier) {
		fClassifier = classifier;
		fExecutionListener = listener;
	}

	public void run(ITestReference[] suites) {
		for (ITestReference suite : suites) {
			if (fShouldStop)
				return;
			suite.run(this);
		}
	}

	public boolean shouldStop() {
		return fShouldStop;
	}

	public void stop() {
		fShouldStop = true;
		for (IStopListener listener : fStopListeners) {
			listener.stop();
		}
	}

	public IListensToTestExecutions getListener() {
		return fExecutionListener;
	}

	public IClassifiesThrowables getClassifier() {
		return fClassifier;
	}

	public void addStopListener(IStopListener listener) {
		fStopListeners.add(listener);
	}
}
