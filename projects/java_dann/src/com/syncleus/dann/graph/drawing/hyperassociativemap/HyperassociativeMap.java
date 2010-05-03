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
package com.syncleus.dann.graph.drawing.hyperassociativemap;

import com.syncleus.dann.*;
import com.syncleus.dann.graph.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.log4j.Logger;
import com.syncleus.dann.graph.drawing.GraphDrawer;
import com.syncleus.dann.math.Vector;

public class HyperassociativeMap<G extends Graph<N, ?>, N> implements GraphDrawer<G,N>
{
	private final G graph;
	private final int dimensions;
	private final ExecutorService threadExecutor;
	private final static Logger LOGGER = Logger.getLogger(HyperassociativeMap.class);
	private final Map<N, Vector> coordinates = Collections.synchronizedMap(new HashMap<N, Vector>());
	private final static Random RANDOM = new Random();

	private final static double EQUILIBRIUM_DISTANCE = 1.0;
	private double learningRate = 0.04;
	private final static double REPULSIVE_WEAKNESS = 2.0;
	private final static double ATTRACTION_STRENGTH = 4.0;

	private class Align implements Callable<Vector>
	{
		private N node;

		public Align(N node)
		{
			this.node = node;
		}

		public Vector call()
		{
			return align(this.node);
		}
	}

	public HyperassociativeMap(G graph, int dimensions, ExecutorService threadExecutor)
	{
		if(graph == null)
			throw new IllegalArgumentException("Graph can not be null");
		if(dimensions <= 0)
			throw new IllegalArgumentException("dimensions must be 1 or more");
		
		this.graph = graph;
		this.dimensions = dimensions;
		this.threadExecutor = threadExecutor;

		//refresh all nodes
		for(N node : this.graph.getNodes())
			this.coordinates.put(node, randomCoordinates(this.dimensions));
	}

	public HyperassociativeMap(G graph, int dimensions)
	{
		this(graph, dimensions, null);
	}

	public G getGraph()
	{
		return this.graph;
	}

	public boolean isAlignable()
	{
		return false;
	}

	public boolean isAligned()
	{
		return false;
	}

	public void align()
	{
		//refresh all nodes
		final Map<N, Vector> newCoordinates = new HashMap<N,Vector>();
		for(N node : this.graph.getNodes())
		{
			if( this.coordinates.containsKey(node) )
				newCoordinates.put(node, this.coordinates.get(node));
			else
				newCoordinates.put(node, randomCoordinates(this.dimensions));
		}
		this.coordinates.clear();
		this.coordinates.putAll(newCoordinates);

		Vector center;
		if(this.threadExecutor != null)
		{
			//align all nodes in parallel
			final List<Future<Vector>> futures = this.submitFutureAligns();

			//wait for all nodes to finish aligning and calculate new sum of all the points
			try
			{
				center = this.waitAndProcessFutures(futures);
			}
			catch(InterruptedException caught)
			{
				LOGGER.warn("waitAndProcessFutures was unexpectidy interupted", caught);
				throw new UnexpectedInterruptedException("Unexpected interuption. Get should block indefinately", caught);
			}
		}
		else
			center = this.processLocally();

		//divide each coordinate of the sum of all the points by the number of
		//nodes in order to calulate the average point, or center of all the
		//points
		for(int dimensionIndex = 1; dimensionIndex <= this.dimensions; dimensionIndex++)
			center = center.setCoordinate(center.getCoordinate(dimensionIndex)/((double)this.graph.getNodes().size()),dimensionIndex);

		this.recenterNodes(center);
	}

	public int getDimensions()
	{
		return this.dimensions;
	}

	public Map<N, Vector> getCoordinates()
	{
		return Collections.unmodifiableMap(this.coordinates);
	}

	private void recenterNodes(final Vector center)
	{
		for(N node : this.graph.getNodes())
			this.coordinates.put(node, this.coordinates.get(node).calculateRelativeTo(center));
	}

	Set<N> getNeighbors(N nodeToQuery)
	{
		return new HashSet<N>(this.graph.getAdjacentNodes(nodeToQuery));
	}

	final private Vector align(N nodeToAlign)
	{
        //calculate equilibrium with neighbors
		final Vector location = this.coordinates.get(nodeToAlign);
		final Set<N> neighbors = this.getNeighbors(nodeToAlign);

        Vector compositeVector = new Vector(location.getDimensions());
        for(N neighbor : neighbors)
        {
            Vector neighborVector = this.coordinates.get(neighbor).calculateRelativeTo(location);
            if (Math.abs(neighborVector.getDistance()) > EQUILIBRIUM_DISTANCE)
			{
				double newDistance = Math.pow(Math.abs(neighborVector.getDistance()) - EQUILIBRIUM_DISTANCE, ATTRACTION_STRENGTH);
				if(Math.abs(newDistance) > Math.abs(Math.abs(neighborVector.getDistance()) - EQUILIBRIUM_DISTANCE))
					newDistance = Math.copySign(Math.abs(Math.abs(neighborVector.getDistance()) - EQUILIBRIUM_DISTANCE), newDistance);
				newDistance *= learningRate;
                neighborVector = neighborVector.setDistance(Math.signum(neighborVector.getDistance()) * newDistance);
			}
            else
			{
				double newDistance = -1.0 * atanh((EQUILIBRIUM_DISTANCE - Math.abs(neighborVector.getDistance())) / EQUILIBRIUM_DISTANCE);
				if( Math.abs(newDistance) > Math.abs(EQUILIBRIUM_DISTANCE - Math.abs(neighborVector.getDistance())))
					newDistance = -1.0 * (EQUILIBRIUM_DISTANCE - Math.abs(neighborVector.getDistance()));
				newDistance *= learningRate;
                neighborVector = neighborVector.setDistance(Math.signum(neighborVector.getDistance()) * newDistance);
			}

            compositeVector = compositeVector.add(neighborVector);
        }

        //calculate repulsion with all non-neighbors
        for (N node : this.graph.getNodes())
            if ((neighbors.contains(node) == false)&&(node != nodeToAlign)&&(this.graph.getAdjacentNodes(node).contains(nodeToAlign) == false) )
            {
                Vector nodeVector = this.coordinates.get(node).calculateRelativeTo(location);
				double newDistance = -1.0/Math.pow(nodeVector.getDistance(), REPULSIVE_WEAKNESS);
				if(Math.abs(newDistance) > Math.abs(EQUILIBRIUM_DISTANCE))
					newDistance = Math.copySign(EQUILIBRIUM_DISTANCE, newDistance);

				newDistance *= learningRate;
                nodeVector = nodeVector.setDistance(newDistance);

                compositeVector = compositeVector.add(nodeVector);
            }

		Vector newLocation = location.add(compositeVector);
		Vector oldLocation = this.coordinates.get(nodeToAlign);
		double moveDistance = Math.abs(newLocation.calculateRelativeTo(oldLocation).getDistance());
		if(moveDistance > EQUILIBRIUM_DISTANCE*3.0)
			this.learningRate *= 0.9;
        this.coordinates.put(nodeToAlign, newLocation);
		return newLocation;
	}

	/**
	 * Obtains a Vector with random coordinates for the specified number of
	 * dimensions.
	 *
	 *
	 * @param dimensions Number of dimensions for the random Vector
	 * @return New random Vector
	 * @since 1.0
	 */
    public static Vector randomCoordinates(int dimensions)
    {
        double[] randomCoords = new double[dimensions];
        for (int randomCoordsIndex = 0; randomCoordsIndex < dimensions; randomCoordsIndex++)
            randomCoords[randomCoordsIndex] = (RANDOM.nextDouble() * 2.0) - 1.0;

        return new Vector(randomCoords);
    }

    private static double atanh(double value)
    {
        return 0.5 * Math.log(Math.abs((value + 1.0) / (1.0 - value)));
    }

	private List<Future<Vector>> submitFutureAligns()
	{
		final ArrayList<Future<Vector>> futures = new ArrayList<Future<Vector>>();
		for(N node : this.graph.getNodes())
			futures.add(this.threadExecutor.submit(new Align(node)));
		return futures;
	}

	private Vector processLocally()
	{
		Vector pointSum = new Vector(this.dimensions);
		for(N node : this.graph.getNodes())
		{
			Vector newPoint = this.align(node);
			for(int dimensionIndex = 1; dimensionIndex <= this.dimensions; dimensionIndex++)
				pointSum = pointSum.setCoordinate(pointSum.getCoordinate(dimensionIndex) + newPoint.getCoordinate(dimensionIndex), dimensionIndex);
		}

		this.learningRate *= 0.99;
		return pointSum;
	}

	private Vector waitAndProcessFutures(final List<Future<Vector>> futures) throws InterruptedException
	{
		//wait for all nodes to finish aligning and calculate new center point
		Vector pointSum = new Vector(this.dimensions);
		try
		{
			for(Future<Vector> future : futures)
			{
				Vector newPoint = future.get();
				for(int dimensionIndex = 1; dimensionIndex <= this.dimensions; dimensionIndex++)
					pointSum = pointSum.setCoordinate(pointSum.getCoordinate(dimensionIndex) + newPoint.getCoordinate(dimensionIndex), dimensionIndex);
			}
		}
		catch(ExecutionException caught)
		{
			LOGGER.error("Align had an unexcepted problem executing.", caught);
			throw new UnexpectedDannError("Unexpected execution exception. Get should block indefinately", caught);
		}
		
		this.learningRate *= 0.99;

		return pointSum;
	}
}
