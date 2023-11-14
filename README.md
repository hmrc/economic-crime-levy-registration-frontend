# economic-crime-levy-registration-frontend

This is the frontend microservice that enables customers to register for the Economic Crime Levy
using the ROSM pattern. Transient journey data is stored in the
backend [economic-crime-levy-registration](https://github.com/hmrc/economic-crime-levy-registration) microservice.

## Running the service

> `sbt run`

The service runs on port `14000` by default.

## Running dependencies

Using [service manager](https://github.com/hmrc/service-manager)
with the service manager profile `ECONOMIC_CRIME_LEVY_ALL` will start
all of the Economic Crime Levy microservices as well as the services
that they depend on.

> `sm --start ECONOMIC_CRIME_LEVY_ALL`

## Running tests

### Unit tests

> `sbt test`

### Integration tests

> `sbt it:test`

### All tests

This is an sbt command alias specific to this project. It will run a scala format
check, run a scala style check, run unit tests, run integration tests and produce a coverage report.
> `sbt runAllChecks`

## Scalafmt and Scalastyle

To check if all the scala files in the project are formatted correctly:
> `sbt scalafmtCheckAll`

To format all the scala files in the project correctly:
> `sbt scalafmtAll`

To check if there are any scalastyle errors, warnings or infos:
> `sbt scalastyle`

## Feature flags

- alfStubEnabled: When enabled we use the stub for the Address Look Up service.
- enrolmentStoreProxyStubEnabled: When enabled we use the stub for the Enrolment Store Proxy service.
- enrolmentStoreProxyStubReturnsEclReference: When enabled we stub the response from the Enrolment Store Proxy service for getting enrolments for a group.
- grsStubEnabled: When enabled we use the stub for the Incorporated Entity Identification, Partnership Identification and Sole Trader Identification services.
- incorporatedEntityBvEnabled: Controls whether we enable business verification in our request to the Incorporated Entity Identification service.
- partnershipBvEnabled: Controls whether we enable business verification in our request to the Partnership Identification service.
- soleTraderBvEnabled: Controls whether we enable business verification in our request to the Sole Trader Identification service.
- welsh-translation: Controls whether the link to view a page in Welsh is displayed on a page.


### License

This code is open source software licensed under
the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").