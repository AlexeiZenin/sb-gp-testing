# Generic Protobuf Processor - Any Kafka Event to your Reporting Warehouse

## Overview

A processor which listens to a configured set of Kafka topics and converts
the Protobuf events to JSON for forwarding to a fictional reporting warehouse/system. Uses
Spring Boot Kafka + Confluent's Schema Registry Protobuf client.

The processor itself contains no explicit models on the classpath for the
events it supports and instead utilizes Confluent's Schema Registry
and Protobuf's `DynamicMessage` class to resolve each event's schema
at runtime. This allows any new Protobuf event to be converted to JSON
without the developer ever having to update this processor.
The only restriction is that the Protobuf schema in the Kafka topic **must** 
be fully self-describing and not require any _"schema-on-read"_ parsing semantics
to handle the event (e.g. using the `Any` construct or raw bytes).

The processor also provides 3 possible strategies for timestamp enhancements, as reporting systems
usually have different needs for timestamp representation.

### Try it out (requires Docker)

The following commands will each execute the generic processor and convert 50 events to JSON
before exiting. During the execution of the integration test the Spring Boot application will be 
brought up along with a Kafka container; which our Spring Boot app will read messages from as if
it were running in a real environment. Each option below will convert the timestamps in the message
differently.

#### Run with UNIX mode
```shell script
Linux: ./mvnw -Dtest=GenericProtoApplicationTest -Dzenin.json.timestampMode=UNIX clean test
Windows: mvnw.cmd -Dtest=GenericProtoApplicationTest -Dzenin.json.timestampMode=UNIX clean test
```

#### Run with GOOGLE mode
```shell script
Linux: ./mvnw -Dtest=GenericProtoApplicationTest -Dzenin.json.timestampMode=GOOGLE clean test
Windows: mvnw.cmd -Dtest=GenericProtoApplicationTest -Dzenin.json.timestampMode=GOOGLE clean test
```

#### Run with PRESERVE_PROTO mode
```shell script
Linux: ./mvnw -Dtest=GenericProtoApplicationTest -Dzenin.json.timestampMode=PRESERVE_PROTO clean test
Windows: mvnw.cmd -Dtest=GenericProtoApplicationTest -Dzenin.json.timestampMode=PRESERVE_PROTO clean test
```

## Context

Protobuf contains a standard type named `google/protobuf/timestamp.proto`. The definition
is below:

```protobuf
syntax = "proto3";

message Timestamp {
  // Represents seconds of UTC time since Unix epoch
  // 1970-01-01T00:00:00Z. Must be from 0001-01-01T00:00:00Z to
  // 9999-12-31T23:59:59Z inclusive.
  int64 seconds = 1;

  // Non-negative fractions of a second at nanosecond resolution. Negative
  // second values with fractions must still have non-negative nanos values
  // that count forward in time. Must be from 0 to 999,999,999
  // inclusive.
  int32 nanos = 2;
}
```

## Design

The above type is the standard way for expressing time values in the Protobuf
IDL. Our generic processor will scan each event's schema tree for any field of the type 
`google/protobuf/timestamp.proto` and enhance the timestamp to conform to 1 of 3 
strategies shown below.

### `GOOGLE` strategy

Serialize all timestamps according to Google's standard Protobuf 
JSON serializer in [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601)
(e.g.`"2020-09-20T23:50:54.322Z"`)

### `UNIX` strategy

Serialize all timestamps as [UNIX epoch](https://en.wikipedia.org/wiki/Unix_time) 
timestamps, measured in seconds and represented as a long (e.g. `1600646110`)

### `PRESERVE_PROTO` strategy 

Serialize all timestamps accoding to the actual 
structural defintion of the underlying `google/protobuf/timestamp.proto` with the
`seconds` and `nanos` fields. See below:

```json
{
  "seconds": 1600646245,
  "nanos": 210000000
}
```

## Configuration

By utilizing the power of Spring Boot we can change the topic pattern we consume events from and can
also specify the type of timestamp enhancement we want our processor to perform. This all can be set
via Spring Boot's various options for injecting configuration but visually illustrated in YAML below:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: "reporting-warehouse"

zenin:
  kafka:
    topic-pattern: "prod\\..*"
  json:
    timestampMode: UNIX
```

The above will make the processor listen to all topics prefixed with `prod` and transform all 
`google/protobuf/timestamp.proto` with the `UNIX` strategy when converting to JSON.
