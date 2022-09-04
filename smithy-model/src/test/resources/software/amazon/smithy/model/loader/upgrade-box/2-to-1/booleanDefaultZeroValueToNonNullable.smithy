// {"v1-box": false, "v1-client-zero-value": false, "v2": false}
$version: "2.0"

namespace smithy.example

structure Foo {
    booleanDefaultZeroValueToNonNullable: MyBoolean = false
}

boolean MyBoolean
