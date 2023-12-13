package edu.uw.bothell.css.dsl.MASS.cypher.functions;

import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherArgumentErrorException;

import java.util.Arrays;
import java.util.stream.Collectors;

public class FunctionUtils {
    public static void assertArgumentCount(Object[] arguments, int... expectedCounts) {
        for (int count : expectedCounts) {
            if (arguments.length == count) {
                return;
            }
        }

        throw new PropertyGraphCypherArgumentErrorException(String.format(
            "Unexpected number of arguments. Expected %s, found %d",
            Arrays.stream(expectedCounts).mapToObj(Integer::toString).collect(Collectors.joining(", ")),
            arguments.length
        ));
    }
}
