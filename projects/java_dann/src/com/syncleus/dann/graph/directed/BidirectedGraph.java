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
package com.syncleus.dann.graph.directed;

import com.syncleus.dann.graph.*;
import java.util.List;
import java.util.Set;

public interface BidirectedGraph<G extends BidirectedGraph<? extends G, ? extends N, ? extends E, ? extends W>, N extends BidirectedNode<? extends E>, E extends BidirectedEdge<? extends N>, W extends BidirectedWalk<? extends N, ? extends E>> extends Graph<G, N, E, W>
{
	Set<? extends N> getNodes();
	List<? extends E> getEdges();
	Set<? extends G> getConnectedComponents();
	boolean isStronglyConnected();
	Set<? extends G> getStrongComponents();
	boolean isPolytree();
}