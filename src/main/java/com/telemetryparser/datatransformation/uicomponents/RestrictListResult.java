package com.telemetryparser.datatransformation.uicomponents;

import java.util.Set;

public record RestrictListResult(Set<String> toRestrict, Integer value)
{
}
