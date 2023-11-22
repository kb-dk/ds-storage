/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.storage.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.*;
import java.util.stream.*;

/**
 * {@code Stream} equivalent of {@link java.io.FilterInputStream} for easy extension of {@link Stream}s.
 * All methods delegates to the given inner {@code Stream}.
 */
public class FilterStream<T> implements Stream<T> {
    private final Stream<T> inner;

    public FilterStream(Stream<T> inner) {
        this.inner = inner;
    }

    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        return inner.filter(predicate);
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> function) {
        return inner.map(function);
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> toIntFunction) {
        return inner.mapToInt(toIntFunction);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> toLongFunction) {
        return inner.mapToLong(toLongFunction);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> toDoubleFunction) {
        return inner.mapToDouble(toDoubleFunction);
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> function) {
        return inner.flatMap(function);
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> function) {
        return inner.flatMapToInt(function);
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> function) {
        return inner.flatMapToLong(function);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> function) {
        return inner.flatMapToDouble(function);
    }

    @Override
    public Stream<T> distinct() {
        return inner.distinct();
    }

    @Override
    public Stream<T> sorted() {
        return inner.sorted();
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        return inner.sorted(comparator);
    }

    @Override
    public Stream<T> peek(Consumer<? super T> consumer) {
        return inner.peek(consumer);
    }

    @Override
    public Stream<T> limit(long l) {
        return inner.limit(l);
    }

    @Override
    public Stream<T> skip(long l) {
        return inner.skip(l);
    }

    @Override
    public Stream<T> takeWhile(Predicate<? super T> predicate) {
        return inner.takeWhile(predicate);
    }

    @Override
    public Stream<T> dropWhile(Predicate<? super T> predicate) {
        return inner.dropWhile(predicate);
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        inner.forEach(consumer);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> consumer) {
        inner.forEachOrdered(consumer);
    }

    @Override
    public Object[] toArray() {
        return inner.toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> intFunction) {
        return inner.toArray(intFunction);
    }

    @Override
    public T reduce(T t, BinaryOperator<T> binaryOperator) {
        return inner.reduce(t, binaryOperator);
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> binaryOperator) {
        return inner.reduce(binaryOperator);
    }

    @Override
    public <U> U reduce(U u, BiFunction<U, ? super T, U> biFunction, BinaryOperator<U> binaryOperator) {
        return inner.reduce(u, biFunction, binaryOperator);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> biConsumer, BiConsumer<R, R> biConsumer1) {
        return inner.collect(supplier, biConsumer, biConsumer1);
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return inner.collect(collector);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return inner.min(comparator);
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        return inner.max(comparator);
    }

    @Override
    public long count() {
        return inner.count();
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return inner.anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        return inner.allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return inner.noneMatch(predicate);
    }

    @Override
    public Optional<T> findFirst() {
        return inner.findFirst();
    }

    @Override
    public Optional<T> findAny() {
        return inner.findAny();
    }

    public static <T1> Builder<T1> builder() {
        return Stream.builder();
    }

    public static <T1> Stream<T1> empty() {
        return Stream.empty();
    }

    public static <T1> Stream<T1> of(T1 t1) {
        return Stream.of(t1);
    }

    public static <T1> Stream<T1> ofNullable(T1 t1) {
        return Stream.ofNullable(t1);
    }

    @SafeVarargs
    public static <T1> Stream<T1> of(T1... values) {
        return Stream.of(values);
    }

    public static <T1> Stream<T1> iterate(T1 seed, UnaryOperator<T1> f) {
        return Stream.iterate(seed, f);
    }

    public static <T1> Stream<T1> iterate(T1 seed, Predicate<? super T1> hasNext, UnaryOperator<T1> next) {
        return Stream.iterate(seed, hasNext, next);
    }

    public static <T1> Stream<T1> generate(Supplier<? extends T1> s) {
        return Stream.generate(s);
    }

    public static <T1> Stream<T1> concat(Stream<? extends T1> a, Stream<? extends T1> b) {
        return Stream.concat(a, b);
    }

    @Override
    public Iterator<T> iterator() {
        return inner.iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return inner.spliterator();
    }

    @Override
    public boolean isParallel() {
        return inner.isParallel();
    }

    @Override
    public Stream<T> sequential() {
        return inner.sequential();
    }

    @Override
    public Stream<T> parallel() {
        return inner.parallel();
    }

    @Override
    public Stream<T> unordered() {
        return inner.unordered();
    }

    @Override
    public Stream<T> onClose(Runnable runnable) {
        return inner.onClose(runnable);
    }

    @Override
    public void close() {
        inner.close();
    }
}
