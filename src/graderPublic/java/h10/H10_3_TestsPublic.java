package h10;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sourcegrade.jagr.api.rubric.TestForSubmission;
import org.tudalgo.algoutils.tutor.general.reflections.BasicTypeLink;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static h10.H10_TestUtilsPublic.*;
import static org.tudalgo.algoutils.tutor.general.match.BasicStringMatchers.identical;

@TestForSubmission
public class H10_3_TestsPublic {

    BasicTypeLink stackLink;
    Method anyMatch;
    Method mapFunctional;


    @BeforeEach
    public void setUpPublic() {
        stackLink = BasicTypeLink.of(Enclosure.class);
        anyMatch = stackLink.getMethod(identical("anyMatch")).reflection();
        mapFunctional = stackLink.getMethod(identical("mapFunctional")).reflection();
    }

    @Test
    public void testAnyMatchParameterPublic() {
        Predicate<Type> typeMatcher =
            getDefinedTypes(Enclosure.class, ".*").stream()
                .map(type -> matchWildcard(false, type))
                .reduce(Predicate::or)
                .orElse(new H10_TestUtilsPublic.GenericPredicate(i -> false, "Expected type is not defined"));

        assertParameters(anyMatch, List.of(matchNested(Predicate.class, typeMatcher)));
    }

    @Test
    public void testMapFunctionalParameterPublic() {
        // Method has two type parameters: B and E (order may vary)
        TypeVariable<?> typeParam0 = mapFunctional.getTypeParameters()[0];
        TypeVariable<?> typeParam1 = mapFunctional.getTypeParameters()[1];

        // Determine which type parameter is E (extends Enclosure) and which is B (extends Animal)
        // by checking their bounds
        TypeVariable<?> typeE;
        TypeVariable<?> typeB;
        if (Arrays.stream(typeParam0.getBounds()).anyMatch(b -> b.getTypeName().contains("Enclosure"))) {
            typeE = typeParam0;
            typeB = typeParam1;
        } else {
            typeE = typeParam1;
            typeB = typeParam0;
        }

        // A from Enclosure<A>
        TypeVariable<?> typeA = Enclosure.class.getTypeParameters()[0];

        Predicate<Type> aMatcher = match(typeA);
        Predicate<Type> bMatcher = match(typeB);
        Predicate<Type> eMatcher = match(typeE);

        // wildcards
        Predicate<Type> superA = matchWildcard(false, aMatcher);
        Predicate<Type> extendsE = matchWildcard(true, eMatcher);
        Predicate<Type> extendsB = matchWildcard(true, bMatcher);

        // Allow both exact type and wildcard variant
        Predicate<Type> eOrExtendsE = eMatcher.or(extendsE);
        Predicate<Type> bOrExtendsB = bMatcher.or(extendsB);

        assertParameters(
            mapFunctional,
            List.of(
                // Supplier<E> or Supplier<? extends E>
                matchNested(Supplier.class, eOrExtendsE),

                // Function<? super A, B> or Function<? super A, ? extends B>
                matchNested(Function.class,
                    superA,       // first parameter: ? super A
                    bOrExtendsB   // second parameter: B or ? extends B
                )
            )
        );
    }

    @Test
    public void testMapFunctionalReturnTypePublic() {
        Predicate<Type> methodTypeMatcher =
            Arrays.stream(mapFunctional.getTypeParameters())
                .map(type -> match(type))
                .reduce(Predicate::or)
                .orElse(new GenericPredicate(i -> false, "Expected method type not defined"));

        assertReturnParameter(mapFunctional, methodTypeMatcher);
    }

}
