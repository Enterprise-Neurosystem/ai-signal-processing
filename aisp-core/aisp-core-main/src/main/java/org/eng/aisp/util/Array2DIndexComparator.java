/*******************************************************************************
 * Copyright [2022] [IBM]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.eng.aisp.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Array2DIndexComparator implements Comparator<IndexPair>
{
	
    private final double[][] array;

    public Array2DIndexComparator(double[][] array)
    {
        this.array = array;
    }

    public IndexPair[] createIndexArray(boolean halfMatrix)
    {
    	if (!halfMatrix)
    		throw new IllegalArgumentException("not implemented");
    	List<IndexPair> indexes = new ArrayList<>();
        for (int i = 0; i < array.length; i++)
        {
            for (int j = i+1; j < array[0].length; j++)
            {
            	indexes.add(new IndexPair(i, j)); 
            }
        }
        return indexes.toArray(new IndexPair[indexes.size()]);
    }

    @Override
    public int compare(IndexPair index1, IndexPair index2)
    {
        double v1 = array[index1.getX()][index1.getY()];
        double v2 = array[index2.getX()][index2.getY()];
        return Double.compare(v1, v2);
    }
}
