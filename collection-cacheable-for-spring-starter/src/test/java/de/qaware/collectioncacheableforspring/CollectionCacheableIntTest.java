package de.qaware.collectioncacheableforspring;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CollectionCacheableIntTest {

    private static final CollectionCacheableTestId SOME_KEY_1 = new CollectionCacheableTestId("some-key-1");
    private static final CollectionCacheableTestValue SOME_VALUE_1 = new CollectionCacheableTestValue("some-value-1");
    private static final CollectionCacheableTestId SOME_KEY_2 = new CollectionCacheableTestId("some-key-2");
    private static final CollectionCacheableTestValue SOME_VALUE_2 = new CollectionCacheableTestValue("some-value-2");
    private static final CollectionCacheableTestId SOME_KEY_3 = new CollectionCacheableTestId("some-key-3");
    private static final CollectionCacheableTestValue SOME_VALUE_3 = new CollectionCacheableTestValue("some-value-3");

    @Autowired
    private CollectionCacheableTestRepository sut;

    @MockBean
    private CollectionCacheableTestDbRepository repository;

    @Autowired
    private CacheManager cacheManager;

    @Before
    public void setUp() throws Exception {
        cacheManager.getCacheNames().stream()
                .map(cacheManager::getCache)
                .filter(Objects::nonNull)
                .forEach(Cache::clear);
    }

    @Test
    public void findById() throws Exception {
        when(repository.findById(SOME_KEY_1)).thenReturn(SOME_VALUE_1);

        // find it two times, but database is only asked once
        assertThat(sut.findById(SOME_KEY_1)).isEqualTo(SOME_VALUE_1);
        assertThat(sut.findById(SOME_KEY_1)).isEqualTo(SOME_VALUE_1);

        verify(repository, times(1)).findById(SOME_KEY_1);
    }

    @Test
    public void findByIds() throws Exception {
        when(repository.findById(SOME_KEY_1)).thenReturn(SOME_VALUE_1);
        when(repository.findById(SOME_KEY_2)).thenReturn(SOME_VALUE_2);

        // find it two times, but database is only asked once
        assertThat(sut.findByIds(setOf(SOME_KEY_1, SOME_KEY_2)))
                .containsOnly(entry(SOME_KEY_1, SOME_VALUE_1), entry(SOME_KEY_2, SOME_VALUE_2));
        assertThat(sut.findByIds(setOf(SOME_KEY_1, SOME_KEY_2)))
                .containsOnly(entry(SOME_KEY_1, SOME_VALUE_1), entry(SOME_KEY_2, SOME_VALUE_2));

        verify(repository, times(1)).findById(SOME_KEY_1);
        verify(repository, times(1)).findById(SOME_KEY_2);
    }

    @Test
    public void findByIdsAfterTwoFindById() throws Exception {
        when(repository.findById(SOME_KEY_1)).thenReturn(SOME_VALUE_1);
        when(repository.findById(SOME_KEY_2)).thenReturn(SOME_VALUE_2);

        // find it two times, but database is only asked once
        assertThat(sut.findById(SOME_KEY_1)).isEqualTo(SOME_VALUE_1);
        assertThat(sut.findById(SOME_KEY_2)).isEqualTo(SOME_VALUE_2);
        assertThat(sut.findByIds(setOf(SOME_KEY_1, SOME_KEY_2)))
                .containsOnly(entry(SOME_KEY_1, SOME_VALUE_1), entry(SOME_KEY_2, SOME_VALUE_2));

        verify(repository, times(1)).findById(SOME_KEY_1);
        verify(repository, times(1)).findById(SOME_KEY_2);
    }

    @Test
    public void findByIdsAfterOneFindById() throws Exception {
        when(repository.findById(SOME_KEY_1)).thenReturn(SOME_VALUE_1);
        when(repository.findById(SOME_KEY_2)).thenReturn(SOME_VALUE_2);

        // find it two times, but database is only asked once
        assertThat(sut.findById(SOME_KEY_1)).isEqualTo(SOME_VALUE_1);
        assertThat(sut.findByIds(setOf(SOME_KEY_1, SOME_KEY_2)))
                .containsOnly(entry(SOME_KEY_1, SOME_VALUE_1), entry(SOME_KEY_2, SOME_VALUE_2));

        verify(repository, times(1)).findById(SOME_KEY_1);
    }

    @Test
    public void findByIdsWithCondition_notFulfilled() throws Exception {
        when(repository.findById(SOME_KEY_1)).thenReturn(SOME_VALUE_1);
        when(repository.findById(SOME_KEY_2)).thenReturn(SOME_VALUE_2);
        when(repository.findById(SOME_KEY_3)).thenReturn(SOME_VALUE_3);

        // the findByIdsWithCondition() won't touch cache as the condition is not met
        assertThat(sut.findById(SOME_KEY_1)).isEqualTo(SOME_VALUE_1);
        assertThat(sut.findByIdsWithCondition(setOf(SOME_KEY_1, SOME_KEY_2, SOME_KEY_3)))
                .containsOnly(entry(SOME_KEY_1, SOME_VALUE_1), entry(SOME_KEY_2, SOME_VALUE_2), entry(SOME_KEY_3, SOME_VALUE_3));

        verify(repository, times(2)).findById(SOME_KEY_1);
    }


    @Test
    public void findByIdsWithCondition_fulfilled() throws Exception {
        when(repository.findById(SOME_KEY_1)).thenReturn(SOME_VALUE_1);
        when(repository.findById(SOME_KEY_2)).thenReturn(SOME_VALUE_2);

        // the findByIdsWithCondition() uses the cache as the condition is met
        assertThat(sut.findById(SOME_KEY_1)).isEqualTo(SOME_VALUE_1);
        assertThat(sut.findByIdsWithCondition(setOf(SOME_KEY_1, SOME_KEY_2)))
                .containsOnly(entry(SOME_KEY_1, SOME_VALUE_1), entry(SOME_KEY_2, SOME_VALUE_2));
        assertThat(sut.findById(SOME_KEY_2)).isEqualTo(SOME_VALUE_2);

        verify(repository, times(1)).findById(SOME_KEY_1);
        verify(repository, times(1)).findById(SOME_KEY_2);
    }

    @Test
    public void findByIdsWithUnless_notFulfilled() throws Exception {
        when(repository.findById(SOME_KEY_1)).thenReturn(SOME_VALUE_1);

        // the findByIdsWithUnless() fills the cache as the unless is not met
        assertThat(sut.findByIdsWithUnless(setOf(SOME_KEY_1)))
                .containsOnly(entry(SOME_KEY_1, SOME_VALUE_1));
        assertThat(sut.findById(SOME_KEY_1)).isEqualTo(SOME_VALUE_1);

        verify(repository, times(1)).findById(SOME_KEY_1);
    }


    @Test
    public void findByIdsWithUnless_fulfilled() throws Exception {
        when(repository.findById(SOME_KEY_1)).thenReturn(SOME_VALUE_1);
        when(repository.findById(SOME_KEY_2)).thenReturn(SOME_VALUE_2);
        when(repository.findById(SOME_KEY_3)).thenReturn(SOME_VALUE_3);

        // the findByIdsWithUnless() does not fill the cache as the unless is met
        assertThat(sut.findById(SOME_KEY_1)).isEqualTo(SOME_VALUE_1);
        assertThat(sut.findByIdsWithUnless(setOf(SOME_KEY_1, SOME_KEY_2, SOME_KEY_3)))
                .containsOnly(entry(SOME_KEY_1, SOME_VALUE_1), entry(SOME_KEY_2, SOME_VALUE_2), entry(SOME_KEY_3, SOME_VALUE_3));
        assertThat(sut.findById(SOME_KEY_2)).isEqualTo(SOME_VALUE_2);

        verify(repository, times(1)).findById(SOME_KEY_1);
        verify(repository, times(2)).findById(SOME_KEY_2);
    }

    @Test
    public void findByIdsWithKey() throws Exception {
        when(repository.findById(SOME_KEY_1)).thenReturn(SOME_VALUE_1);
        when(repository.findById(SOME_KEY_2)).thenReturn(SOME_VALUE_2);

        assertThat(sut.findByIdsWithKey(setOf(SOME_KEY_1)))
                .containsOnly(entry(SOME_KEY_1, SOME_VALUE_1));
        assertThat(sut.findByIdsWithKey(setOf(SOME_KEY_1)))
                .containsOnly(entry(SOME_KEY_1, SOME_VALUE_1));

        assertThat(sut.findByIdWithKey(SOME_KEY_2)).isEqualTo(SOME_VALUE_2);
        assertThat(sut.findByIdsWithKey(setOf(SOME_KEY_2)))
                .containsOnly(entry(SOME_KEY_2, SOME_VALUE_2));

        verify(repository, times(1)).findById(SOME_KEY_1);
        verify(repository, times(1)).findById(SOME_KEY_2);
    }

    @Test
    public void findAll() throws Exception {
        when(repository.findAll()).thenReturn(mapOf(SOME_KEY_1, SOME_VALUE_1));

        // the findAll() fills the cache already!
        assertThat(sut.findAll()).containsOnly(entry(SOME_KEY_1, SOME_VALUE_1));
        assertThat(sut.findByIds(setOf(SOME_KEY_1))).containsOnly(entry(SOME_KEY_1, SOME_VALUE_1));

        verify(repository, never()).findById(any());
    }

    @Test
    public void findAllWithUnless_notFulfilled() throws Exception {
        when(repository.findAll()).thenReturn(mapOf(SOME_KEY_1, SOME_VALUE_1));

        // the findAllWithUnless() fills the cache as the unless is not met
        assertThat(sut.findAllWithUnless()).containsOnly(entry(SOME_KEY_1, SOME_VALUE_1));
        assertThat(sut.findByIds(setOf(SOME_KEY_1))).containsOnly(entry(SOME_KEY_1, SOME_VALUE_1));

        verify(repository, never()).findById(any());
    }

    @Test
    public void findAllWithUnless_fulfilled() throws Exception {
        when(repository.findById(SOME_KEY_1)).thenReturn(SOME_VALUE_1);
        when(repository.findAll()).thenReturn(mapOf(SOME_KEY_1, SOME_VALUE_1, SOME_KEY_2, SOME_VALUE_2));

        // the findAllWithUnless() does not fill the cache already, as the unless is met
        assertThat(sut.findAllWithUnless()).containsOnly(entry(SOME_KEY_1, SOME_VALUE_1), entry(SOME_KEY_2, SOME_VALUE_2));
        assertThat(sut.findByIds(setOf(SOME_KEY_1))).containsOnly(entry(SOME_KEY_1, SOME_VALUE_1));

        verify(repository, times(1)).findById(SOME_KEY_1);
    }

    @Test
    public void findAllWithKey() throws Exception {
        when(repository.findAll()).thenReturn(mapOf(SOME_KEY_1, SOME_VALUE_1));

        assertThat(sut.findAllWithKey()).containsOnly(entry(SOME_KEY_1, SOME_VALUE_1));
        assertThat(sut.findByIdsWithKey(setOf(SOME_KEY_1))).containsOnly(entry(SOME_KEY_1, SOME_VALUE_1));

        verify(repository, never()).findById(any());
    }

    @SpringBootConfiguration
    @EnableCaching
    @EnableAutoConfiguration
    @Import({CollectionCacheableTestRepository.class})
    public static class TestConfig {

    }

    @SafeVarargs
    private static <T> Set<T> setOf(T... items) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(items)));
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> mapOf(Object... keyValues) {
        HashMap<K, V> map = new HashMap<>();
        for (int i = 0; i < keyValues.length / 2; i++) {
            map.put((K) keyValues[2 * i], (V) keyValues[2 * i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
