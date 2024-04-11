# industry-classification-lookup-frontend

[![Build Status](https://travis-ci.org/hmrc/industry-classification-lookup-frontend.svg)](https://travis-ci.org/hmrc/industry-classification-lookup-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/industry-classification-lookup-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/industry-classification-lookup-frontend/_latestVersion)

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

## 1. Starting the service

### With Service Manager
To start all the dependent services required by Industry Classification Lookup Frontend in service manager, run `sm2 --start ICL_ALL`.

If you want to run the service individually, run `sm2 --start ICL_FE`.

Prior to starting the service locally, make sure the instance running in service manager is stopped by running `sm2 --stop ICL_FE`.

### From source code on your local machine
> The below instructions are for Mac/Linux only. Windows users will have to use SBT to run the service on port `9874`.
1. Clone this repository into the development environment on your machine.
2. Open a terminal and navigate to the folder you cloned the service into.
3. Run either `./run.sh` or `sbt "run 9874 -Dapplication.router=testOnlyDoNotUseInAppConf.Routes -Dconfig.resource=application.conf"` to start the service locally on port `9874` and enable the `/test-only` routes for local testing.
4. Ensure all dependent services are started, using the `ICL_ALL` service manager profile.

### Test the application

#### Unit and Integration tests
To run the unit and integration tests, you can either use ```sbt test it/test``` or ```sbt clean coverage test it/test scalastyle coverageReport```.

#### Accessibility tests
To run the accessibility tests, use ```sbt a11y:test```.

## 2. Initializing a Journey
To start, the calling service must make a **POST** to `/internal/initialise-journey`

The **POST** will contain a **json** body as config for setting up the journey. The **json** consists of:
* a `redirect url` which is used by this service to redirect the user back to the calling service once they have confirmed their sic codes
* a `journeySetupDetails` json data which has following **optional** configurations:
  * `queryBooster` - **optional**: true/false, if `true` the first word is boosted to bring results with that term closer to the top of the results else simple query
  * `amountOfResults` - **optional**: set an `int` if nothing provided it will be defaulted to `50`
  * `customMessages` - **optional**: json data to define custom messages to be displayed in Industry Classification Lookup frontend
  * `sicCodes` - **optional**: set a list of `String` sic codes if nothing provided it will be defaulted to an empty list, ie. [ "12345", "67890" ]

The `customMessages` should look like:
```json
{
  "summary": {                              //**optional** - custom messages for the Summary page
    "heading": "Some header content",       //**optional** - define custom heading to be displayed
    "lead": "some lead content paragraph",  //**optional** - define custom lead paragraph content to be displayed
    "hint": "some hint text"                //**optional** - define hint content to be displayed
  }
}
```

The **POST Json** should look like:
```json
{
  "redirectUrl":"return-url-here",
  "queryBooster": true,
  "amountOfResults": 150,
  "customMessages": {
    "summary": {
      "heading": "Some header content",
      "lead": "some lead content paragraph",
      "hint": "some hint text"              
    }
  },
  "sicCodes": [ "12345", "67890" ]
}
```

From this **POST**, the calling service will receive back an **Ok** response if initialisation was successful.

If the response code received is a **BadRequest**, the error message can be seen in the **body** of the response.

Within the body of the **Ok** response is a **json** body which contains:
* a `journeyStartUri`: url that the calling service can redirect to, in order to begin the journey
* a `fetchResultsUri` url which can be used by the calling service to fetch the sic code choices the user has made.

The **Ok** response will look like:
```json
{
  "journeyStartUri":"/sic-search/UUID/search-standard-industry-classification-codes",
  "fetchResultsUri":"/internal/UUID/fetch-results"
}
```

## 3. Receiving selected sic codes
Once the user has left the service and redirected back to the calling service via the provided redirect url, the calling service can make a **GET** request to the **fetchResultsUri** and a json body will be returned with the results of the journey.

The responses that can be recieved from the **GET** can be:

* **Ok** - contains json body of sic code results (see below)
* **BadRequest** - error message can be found in the body of the response
* **NotFound** - initialisation was not done or no sic codes have been selected

The **GET Ok** response json body will look like:
```json
{
  "sicCodes":[
    {
      "code":"12345",
      "desc":"description of code",
      "indexes":[
        "index",
        "index"
      ]
    }
  ]
}
```

The indexes list in the json can be empty on the **GET** 

### Formatting code
This library uses [Scalafmt](https://scalameta.org/scalafmt/), a code formatter for Scala. The formatting rules configured for this repository are defined within [.scalafmt.conf](.scalafmt.conf). Prior to checking in any changes to this repository, please make sure all files are formatted correctly.

To apply formatting to this repository using the configured rules in [.scalafmt.conf](.scalafmt.conf) execute:

```
sbt scalafmtAll
```

To check files have been formatted as expected execute:

```
sbt scalafmtCheckAll scalafmtSbtCheck
```
