FBOT
====

Hackish bot to login into fronter, retrieve PDF's, logout, convert them to html and then reprocess the output to real viewer friendly html. And add a timestamp into the PDFs, so a html and pdf version can be served.
This bot is not intended to be of any use outside it's specific case and would need a rewrite to serve as a good code example.

### Requirements
- Apache httpcomponents-client-4.3.2
  - commons-codec
  - commons-logging
  - httpclient
  - fluent-hc
- itext-pdfa-5.4.3
- itextpdf-5.4.3
- itext-xtra-5.4.3