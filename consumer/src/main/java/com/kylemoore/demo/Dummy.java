package com.kylemoore.demo;

import com.google.common.collect.Lists;

import java.util.Arrays;

public class Dummy {

  public Dummy() {
    System.out.println(Lists.reverse(Arrays.asList("foo", "bar")));
  }

}
