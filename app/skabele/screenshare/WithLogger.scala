package skabele.screenshare

import org.slf4j.LoggerFactory

trait WithLogger {
  protected val logger = LoggerFactory.getLogger(getClass)
}
