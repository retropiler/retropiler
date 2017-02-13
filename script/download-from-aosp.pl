#!/usr/bin/perl
use 5.10.0;
use strict;
use warnings;
use File::Path qw(make_path);
use File::Basename qw(dirname);

my $url_template = "https://android.googlesource.com/platform/libcore/+/%s/ojluni/src/main/java/%s?format=TEXT";;

my @files = (

    (map { "java/util/$_" } qw(
        Optional.java
        OptionalDouble.java
        OptionalInt.java
        OptionalLong.java
    )),

    (map { "java/util/function/$_" } qw(
        BiConsumer.java
       BiFunction.java
       BiPredicate.java
       BinaryOperator.java
       BooleanSupplier.java
       Consumer.java
       DoubleBinaryOperator.java
       DoubleConsumer.java
       DoubleFunction.java
       DoublePredicate.java
       DoubleSupplier.java
       DoubleToIntFunction.java
       DoubleToLongFunction.java
       DoubleUnaryOperator.java
       Function.java
       IntBinaryOperator.java
       IntConsumer.java
       IntFunction.java
       IntPredicate.java
       IntSupplier.java
       IntToDoubleFunction.java
       IntToLongFunction.java
       IntUnaryOperator.java
       LongBinaryOperator.java
       LongConsumer.java
       LongFunction.java
       LongPredicate.java
       LongSupplier.java
       LongToDoubleFunction.java
       LongToIntFunction.java
       LongUnaryOperator.java
       ObjDoubleConsumer.java
       ObjIntConsumer.java
       ObjLongConsumer.java
       Predicate.java
       Supplier.java
       ToDoubleBiFunction.java
       ToDoubleFunction.java
       ToIntBiFunction.java
       ToIntFunction.java
       ToLongBiFunction.java
       ToLongFunction.java
       UnaryOperator.java
       package-info.java
    ))
);

foreach my $file(@files) {
    my $url = sprintf $url_template, 'master', $file;
    say "Downloading $file";
    make_path(dirname("runtime/src/main/java/$file"));
    system qq{curl -L "$url" | base64 -d > "runtime/src/main/java/$file"};
}
