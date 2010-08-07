/******************************************************************************
 *                                                                             *
 *  Copyright: (c) Syncleus, Inc.                                              *
 *                                                                             *
 *  You may redistribute and modify this source code under the terms and       *
 *  conditions of the Open Source Community License - Type C version 1.0       *
 *  or any later version as published by Syncleus, Inc. at www.syncleus.com.   *
 *  There should be a copy of the license included with this file. If a copy   *
 *  of the license is not included you are granted no right to distribute or   *
 *  otherwise use this file except through a legal and valid license. You      *
 *  should also contact Syncleus, Inc. at the information below if you cannot  *
 *  find a license:                                                            *
 *                                                                             *
 *  Syncleus, Inc.                                                             *
 *  2604 South 12th Street                                                     *
 *  Philadelphia, PA 19148                                                     *
 *                                                                             *
 ******************************************************************************/
package com.syncleus.dann.classify.naive.bayes.fisher;

import com.syncleus.dann.classify.naive.bayes.NaiveBayesClassifier;

/**
 * A FisherClassifier is a NaiveBayesClassifier drawing classification from different sources.
 * @param <I> The item to classify
 * @param <F> The fields to classify by
 * @param <C> The category to classify into
 */
public interface FisherClassifier<I, F, C> extends NaiveBayesClassifier<I, F, C>
{
	/**
	 * Sets the minimum level for a given category.
	 * @param category The category
	 * @param minimum The minimum value.
	 */
	void setMinimum(C category, double minimum);

	/**
	 * Gets the minimum level for a given category.
	 * @param category The category
	 * @return The minimum level for the given category
	 */
	double getMinimum(C category);
}
