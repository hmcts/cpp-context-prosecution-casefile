package uk.gov.moj.cpp.prosecution.casefile.mutator;

import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.prosecution.casefile.state.CaseAggregateState;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Defines the composite {@link AggregateStateMutator} which delegates to the appropriate mutator
 * based on the event handled.
 */
final class CompositeCaseAggregateStateMutator implements AggregateStateMutator<Object, CaseAggregateState> {

    static final CompositeCaseAggregateStateMutator INSTANCE = new CompositeCaseAggregateStateMutator();

    private final Map<Class, AggregateStateMutator> eventToStateMutator;

    @SuppressWarnings({"deprecation", "squid:S1602"})
    private CompositeCaseAggregateStateMutator() {
        this.eventToStateMutator = ImmutableMap.<Class, AggregateStateMutator>builder()
                .build();
    }

    @Override
    public void apply(final Object event, final CaseAggregateState aggregateState) {
        ofNullable(eventToStateMutator.get(event.getClass()))
                .ifPresent(stateMutator -> stateMutator.apply(event, aggregateState));
    }

}
