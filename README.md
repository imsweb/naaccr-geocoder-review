# NAACCR Geocoder Review

A simple interface to review [NAACCR Geocoder](https://www.naaccr.org/gis-resources/#Geocoder) results.

Download the latest version from the [Release page](https://github.com/imsweb/naaccr-geocoder-review/releases).

The software is released as an executable JAR file; there is no installation required. Download the latest "-all" JAR file available and double-click it to start the application.

The application only supports input CSV files that have been processed by the NAACCR Geocoder. It expects the Geocoder results to be available in JSON in a column "OutputGeocodes".

For each CSV row, the GUI presents the address that was sent to the Geocoder and the corresponding results. The user is allowed to confirm the "best" result from the Geocoder (the first one), or to select a different one. The user can also reject all the results.

There are three possible output for a CSV line:
1. The line is copied as-is (if the user confirmed the results).
2. The line is copied as-is with the exception of the following fields (if the user rejected all the results, or there were no results available):
     -  naaccrQualCode is set to 99
     - naaccrQualType is set to "Ungeocodable"
     - naaccrCertCode is set to 9
     - naaccrCertType is set to "Ungeocodable"
3. The line has most of the columns overwritten (if the user selected a different result); note that some columns are not mapped in the JSON results, those are still copied as-is.

Regardless of the output, the application always add the following columns at the end of the line:
1. Review App Version (the version of the software that created the output file)
2. Processing Status
    - 0: user confirmed best result
    - 1: user selected a different result
    - 2: user rejected all the results
    - 3: no results were returned by the Geocoder
    - 4: user skipped the line
3. Selected Result Index (the result index selected by the user, -1 if not applicable)
4. Processing Comment (a comment provided by the user)

The full review of a given file is expected to happen in two phases:
1. A first review during which the has the opportunity to skip problem cases.
2. A second review where only skipped lines are presented to the user.
A full review is completed only when those two phases are completed (so when there is no more skipped lines to process).

This application is provided by the [North American Association of Central Cancer Registries](https://www.naaccr.org/)

It was developed by [Information Management Services, Inc.](https://www.imsweb.com/)



