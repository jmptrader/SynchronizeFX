/**
 * This file is part of SynchronizeFX.
 * 
 * Copyright (C) 2013-2014 Saxonia Systems AG
 *
 * SynchronizeFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SynchronizeFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SynchronizeFX. If not, see <http://www.gnu.org/licenses/>.
 */

package de.saxsys.synchronizefx.core.metamodel.executors.lists;

import java.util.UUID;
import static java.util.UUID.randomUUID;

import de.saxsys.synchronizefx.core.metamodel.ListPropertyMetaDataStore;
import de.saxsys.synchronizefx.core.metamodel.ListPropertyMetaDataStore.ListPropertyMetaData;
import de.saxsys.synchronizefx.core.metamodel.TemporaryReferenceKeeper;
import de.saxsys.synchronizefx.core.metamodel.WeakObjectRegistry;
import de.saxsys.synchronizefx.core.metamodel.commands.AddToList;
import de.saxsys.synchronizefx.core.metamodel.commands.ListCommand.ListVersionChange;
import de.saxsys.synchronizefx.core.metamodel.commands.RemoveFromList;
import de.saxsys.synchronizefx.core.metamodel.commands.ReplaceInList;
import de.saxsys.synchronizefx.core.metamodel.commands.Value;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Checks if {@link ListPropertyCommandFilter} works as expected.
 * 
 * @author Raik Bieniek
 */
@RunWith(MockitoJUnitRunner.class)
public class ListPropertyCommandFilterTest {

    private static final UUID EXEMPLARY_LIST_ID = randomUUID();
    private static final UUID EXEMPLARY_OBSERVABLE_OBJECT_ID_1 = randomUUID();
    private static final UUID EXEMPLARY_OBSERVABLE_OBJECT_ID_2 = randomUUID();

    private static final UUID EXEMPLARY_VERSION_FOR_ADD = randomUUID();
    private static final UUID EXEMPLARY_VERSION_FOR_REMOVE = randomUUID();
    private static final UUID EXEMPLARY_VERSION_FOR_REPLACE = randomUUID();

    private static final Value EXEMPLARY_VALUE_FOR_ADD = new Value(EXEMPLARY_OBSERVABLE_OBJECT_ID_1);
    private static final Value EXEMPLARY_VALUE_FOR_REPLACE = new Value(EXEMPLARY_OBSERVABLE_OBJECT_ID_2);

    private final AddToList exemplaryAddToListCommand = new AddToList(EXEMPLARY_LIST_ID,
            new ListVersionChange(EXEMPLARY_VERSION_FOR_ADD, randomUUID()), EXEMPLARY_VALUE_FOR_ADD, 8);
    private final RemoveFromList exemplaryRemoveFromListCommand = new RemoveFromList(EXEMPLARY_LIST_ID,
            new ListVersionChange(EXEMPLARY_VERSION_FOR_REMOVE, randomUUID()), 50, 7);
    private final ReplaceInList exemlaryReplaceInListCommand = new ReplaceInList(EXEMPLARY_LIST_ID,
            new ListVersionChange(EXEMPLARY_VERSION_FOR_REPLACE, randomUUID()), EXEMPLARY_VALUE_FOR_REPLACE, 6);

    @Mock
    private ListPropertyCommandExecutor executor;

    @Mock
    private TemporaryReferenceKeeper referenceKeeper;

    @Mock
    private ListPropertyMetaDataStore listVersions;

    @Mock
    private WeakObjectRegistry objectRegistry;

    private ListPropertyCommandFilter cut;

    /**
     * Sets up the class under test.
     */
    @Before
    public void setUpCut() {
        cut = new ListPropertyCommandFilter(executor, referenceKeeper, listVersions, objectRegistry, false);
    }

    /**
     * Commands that can be applied on the current version of the local list property are executed.
     */
    @Test
    public void executesCommandsThatsVersionMatchesTheVersionOfTheLocalListProperty() {
        when(listVersions.getMetaDataOrFail(EXEMPLARY_LIST_ID))
                .thenReturn(new ListPropertyMetaData(null, EXEMPLARY_VERSION_FOR_ADD))
                .thenReturn(new ListPropertyMetaData(null, EXEMPLARY_VERSION_FOR_REMOVE))
                .thenReturn(new ListPropertyMetaData(null, EXEMPLARY_VERSION_FOR_REPLACE));

        cut.execute(exemplaryAddToListCommand);
        cut.execute(exemplaryRemoveFromListCommand);
        cut.execute(exemlaryReplaceInListCommand);

        verify(executor).execute(exemplaryAddToListCommand);
        verify(executor).execute(exemplaryRemoveFromListCommand);
        verify(executor).execute(exemlaryReplaceInListCommand);

        verify(referenceKeeper, never()).keepReferenceTo(any());
    }

    /**
     * Commands that can not be applied on the currently approved version of the list property are dropped.
     */
    @Test
    public void droppsCommandsThatsVersionDifferFromTheApprovedVersionOfTheListProperty() {
        when(listVersions.getMetaDataOrFail(EXEMPLARY_LIST_ID)).thenReturn(new ListPropertyMetaData(null, randomUUID()))
                .thenReturn(new ListPropertyMetaData(null, randomUUID()))
                .thenReturn(new ListPropertyMetaData(null, randomUUID()));

        cut.execute(exemplaryAddToListCommand);
        cut.execute(exemplaryRemoveFromListCommand);
        cut.execute(exemlaryReplaceInListCommand);

        verifyNoMoreInteractions(executor);
    }

    /**
     * When requested, the cut uses the local version instead of the approved version to filter list commands.
     */
    @Test
    public void canUseLocalVersionInsteadOfApprovedVersionWhenRequested() {
        cut = new ListPropertyCommandFilter(executor, referenceKeeper, listVersions, objectRegistry, true);

        when(listVersions.getMetaDataOrFail(EXEMPLARY_LIST_ID))
                .thenReturn(new ListPropertyMetaData(EXEMPLARY_VERSION_FOR_ADD, null))
                .thenReturn(new ListPropertyMetaData(null, EXEMPLARY_VERSION_FOR_REMOVE));

        cut.execute(exemplaryAddToListCommand);
        cut.execute(exemplaryRemoveFromListCommand);

        verify(executor).execute(exemplaryAddToListCommand);
        verifyNoMoreInteractions(executor);
    }

    /**
     * If dropped {@link AddToList} and {@link ReplaceInList} commands contain references to observable objects they
     * are cached.
     * 
     * <p>
     * This prevents them from being garbage collected before the remote peer send the repaired version of the
     * command.
     * </p>
     */
    @Test
    public void cachesObservableObjectsOfAddToListAndReplaceInListCommands() {
        when(listVersions.getMetaDataOrFail(EXEMPLARY_LIST_ID)).thenReturn(new ListPropertyMetaData(null, randomUUID()))
                .thenReturn(new ListPropertyMetaData(null, randomUUID()));

        final Object observableObjectForAdd = new Object();
        final Object observableObjectForReplace = new Object();

        when(objectRegistry.getByIdOrFail(EXEMPLARY_OBSERVABLE_OBJECT_ID_1)).thenReturn(observableObjectForAdd);
        when(objectRegistry.getByIdOrFail(EXEMPLARY_OBSERVABLE_OBJECT_ID_2)).thenReturn(observableObjectForReplace);

        cut.execute(exemplaryAddToListCommand);
        cut.execute(exemlaryReplaceInListCommand);

        verify(referenceKeeper).keepReferenceTo(observableObjectForAdd);
        verify(referenceKeeper).keepReferenceTo(observableObjectForReplace);
    }

    /**
     * If dropped {@link AddToList} and {@link ReplaceInList} commands contain a simple objects they are not cached.
     * 
     * <p>
     * Simple objects will be resent with the repaired version of the command. It is therefore OK when they are
     * garbage collected.
     * </p>
     */
    @Test
    public void doesntCacheSimpleObjectsOfAddToListAndReplaceInListCommands() {
        when(listVersions.getMetaDataOrFail(EXEMPLARY_LIST_ID)).thenReturn(new ListPropertyMetaData(null, randomUUID()))
                .thenReturn(new ListPropertyMetaData(null, randomUUID()));

        final AddToList addCommandWithSimpleObject = new AddToList(EXEMPLARY_LIST_ID,
                new ListVersionChange(randomUUID(), randomUUID()), new Value("some simple object"), 6);
        final ReplaceInList replaceCommandWithSimpleObject = new ReplaceInList(EXEMPLARY_LIST_ID,
                new ListVersionChange(randomUUID(), randomUUID()), new Value("other simple object"), 7);

        cut.execute(addCommandWithSimpleObject);
        cut.execute(replaceCommandWithSimpleObject);

        verify(referenceKeeper, never()).keepReferenceTo(any());
        verifyNoMoreInteractions(objectRegistry, executor);
    }

    /**
     * The cut triggers a reference clean up on every received list command.
     */
    @Test
    public void cleansUpReferenceCacheOnEveryCommand() {
        when(listVersions.getMetaDataOrFail(EXEMPLARY_LIST_ID))
                .thenReturn(new ListPropertyMetaData(null, randomUUID()));

        cut.execute(exemplaryAddToListCommand);
        cut.execute(exemplaryRemoveFromListCommand);
        cut.execute(exemlaryReplaceInListCommand);

        verify(referenceKeeper, times(3)).cleanReferenceCache();
    }

}
