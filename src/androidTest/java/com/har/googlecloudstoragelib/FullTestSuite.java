package com.har.googlecloudstoragelib;

import android.test.suitebuilder.TestSuiteBuilder;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Created by hareesh on 6/17/15.
 */
public class FullTestSuite extends TestSuite {
  public static Test suite() {
    return new TestSuiteBuilder(FullTestSuite.class)
        .includeAllPackagesUnderHere().build();
  }
}
