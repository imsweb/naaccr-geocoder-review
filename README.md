# NAACCR Geocoder Review

A simple interface to review [NAACCR Geocoder](https://www.naaccr.org/gis-resources/#Geocoder) results.

``This project has been discontinued.``


The software is released as a zip file; there is no installation required. Download the latest "-all" zip file available, unzip it, and double-click the contained executable file to start the application.

The application only supports input CSV files that have been processed by the NAACCR Geocoder. It expects the Geocoder results to be available in JSON in a column "OutputGeocodes".

For each CSV row, the GUI presents the address that was sent to the Geocoder and the corresponding results. The user is allowed to confirm the "best" result from the Geocoder (the first one), or to select a different one. The user can also reject all the results.

If the user selects a different Geocoder result in the interface, the corresponding CSV line in the output line will have its columns updated; only the columns mapped to a JSON field will be updated though.

Some fields are hard-coded depending on the action that the user takes in the interface:
1. If the user rejects all the results or if the Geocoder returns no results:
     - naaccrQualCode is set to 99
     - naaccrQualType is set to "Ungeocodable"
     - naaccrCertCode is set to 9
     - naaccrCertType is set to "Ungeocodable"
     - MicroMatchStatus is set to "X"
2. If the user confirms the best Geocoder results or select a different one:
     - MicroMatchStatus is set to "I"

Regardless of the action the users takes in the interface, the application always adds the following columns at the end of the line:
1. Review App Version (the version of the software that created the output file)
2. Processing Status
    - 0: user confirmed best result
    - 1: user selected a different result
    - 2: user rejected all the results
    - 3: no results were returned by the Geocoder
    - 4: user skipped the line
    - 5: the input had a MicroMatchStatus of 'Match' and the user did not review the result
3. Selected Result Index (the result index selected by the user, -1 if not applicable)
4. Processing Comment (a comment provided by the user)

The full review of a given file is expected to happen in two phases:
1. A first review during which the has the opportunity to skip problem cases.
2. A second review where only skipped lines are presented to the user.
A full review is completed only when those two phases are completed (so when there is no more skipped lines to process).

This application is provided by the [North American Association of Central Cancer Registries](https://www.naaccr.org/)

It was developed by [Information Management Services, Inc.](https://www.imsweb.com/)



