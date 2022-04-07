package org.eng.aisp.transform;

import java.util.List;

public class LogScalingTransformTest extends AbstractWindowTransformTest {
	@Override
	protected void getTransforms(List<ITrainingWindowTransform<double[]>> transforms, List<Integer> expectedCounts) {
		transforms.add( new LogScalingTrainingWindowTransform(0.1));	expectedCounts.add(1);
		
	}

	@Override
	protected boolean isDurationPreserved() { return true;  }
}
