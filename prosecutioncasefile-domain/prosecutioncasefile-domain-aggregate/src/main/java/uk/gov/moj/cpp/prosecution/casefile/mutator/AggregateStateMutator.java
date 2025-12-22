package uk.gov.moj.cpp.prosecution.casefile.mutator;

import uk.gov.moj.cpp.prosecution.casefile.state.AggregateState;
import uk.gov.moj.cpp.prosecution.casefile.state.CaseAggregateState;

/**
 * Defines the mechanism for mutating the {@link AggregateState} based on specific events.
 *
 * @param <T> the event type
 * @param <K> the aggregate type
 */
@FunctionalInterface
public interface AggregateStateMutator<T, K extends AggregateState> {

    /**
     * Mutates the state based on the event.
     *
     * @param event the event to use the details of when mutating the state
     * @param aggregateState the current aggregate state
     */
    void apply(T event, K aggregateState);

    static AggregateStateMutator<Object, CaseAggregateState> compositeCaseAggregateStateMutator() {
        return CompositeCaseAggregateStateMutator.INSTANCE;
    }
}
