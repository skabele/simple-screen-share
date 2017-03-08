import javax.inject.Inject

import play.api.http.DefaultHttpFilters

import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter
import play.filters.hosts.AllowedHostsFilter

/**
  * Add the following filters by default to all projects
  *
  * https://www.playframework.com/documentation/latest/SecurityHeaders
  */
class Filters @Inject()(securityHeadersFilter: SecurityHeadersFilter) extends DefaultHttpFilters(securityHeadersFilter)