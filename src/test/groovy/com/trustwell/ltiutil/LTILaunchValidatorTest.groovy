package com.trustwell.ltiutil

import spock.lang.Specification

import java.time.Instant
import java.util.concurrent.TimeUnit

@SuppressWarnings("GroovyAccessibility")
class LTILaunchValidatorTest extends Specification {

    def "Test validate signature"() {
        given:
        String baseString = "POST&http%3A%2F%2Fwww.test.com%2F&oauth_consumer_key%3Dkey%26oauth_nonce%3D123%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D378691200%26oauth_version%3D1.0"
        String secret = "secret"
        String signatureMethod = "HMAC-SHA1"

        expect:
        expectedResult == LTILaunchValidator.validateSignature(signature, baseString, secret, signatureMethod)

        where:
        expectedResult  | signature
        true            | "wsXWIddhLVZY7Payf1fZ7laCbXA="
        false           | "wsXWIddhLRZY7Payf1fZ7laCbXA="
    }

    def "Test validate signature - Exception"() {
        when:
        boolean returnedValue = LTILaunchValidator.validateSignature(null, null, null, "sig")

        then:
        !returnedValue
    }

    def "Test construct base string"() {
        given:
        String method = "POST"
        String url = "http://www.test.com/"
        Map<String, String> requestParameters = [
                "oauth_signature": "sign",
                "oauth_consumer_key": "key",
                "oauth_nonce": "123",
                "oauth_signature_method": "HMAC-SHA1",
                "oauth_timestamp": "378691200",
                "oauth_version": "1.0"
        ]

        when:
        String returnedString = LTILaunchValidator.constructBaseString(method, url, requestParameters)

        then:
        "POST&http%3A%2F%2Fwww.test.com%2F&oauth_consumer_key%3Dkey%26oauth_nonce%3D123%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D378691200%26oauth_version%3D1.0" == returnedString
    }

    def "Test normalize and concatenate parameters"() {
        given:
        Map<String, String> parameters = ["parameter3": "value3", "parameter1": "value1", "parameter2": "value2"]

        when:
        String returnedString = LTILaunchValidator.normalizeAndConcatenateParameters(parameters)

        then:
        "parameter1=value1&parameter2=value2&parameter3=value3" == returnedString
    }

    def "Test validate required fields"() {
        expect:
        expectedResult == LTILaunchValidator.validateRequiredFields(requestParameters)

        where:
        expectedResult  | requestParameters
        true            | ["oauth_consumer_key": "key", "oauth_nonce": "123", "oauth_signature_method": "HMAC-SHA1", "oauth_timestamp": Instant.now().getEpochSecond().toString(), "oauth_version": "1.0"]
        false           | ["oauth_version": "1.0"]
    }

    def "Test timestamp is less than five minutes old"() {
        expect:
        valid == LTILaunchValidator.validateTimeStampLessThanFiveMinutes(timestamp)

        where:
        valid   | timestamp
        true    | Instant.now().getEpochSecond().toString()
        false   | (Instant.now().getEpochSecond() - TimeUnit.MINUTES.toSeconds(6)).toString()
        true    | (Instant.now().getEpochSecond() - TimeUnit.MINUTES.toSeconds(5)).toString()
    }

    def "Test normalize url"() {
        expect:
        returnedUrl == LTILaunchValidator.normalizeUrl(url)

        where:
        url                         |   returnedUrl
        "http://www.test.com"       |   "http://www.test.com"
        "http://www.test.com:75"    |   "http://www.test.com:75"
        "http://www.test.com:80"    |   "http://www.test.com"
        "https://www.test.com:80"   |   "https://www.test.com:80"
        "https://www.test.com:443"  |   "https://www.test.com"
        "not a url"                 |   ""
    }


    def "Test encode"() {
        given:
        String testString = "test me*~"

        when:
        String returnedString = LTILaunchValidator.encode(testString)

        then:
        !returnedString.contains("+")
        !returnedString.contains("*")
        !returnedString.contains("%7E")
        "test%20me%2A~" == returnedString
    }
}
