/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package ninja.errors;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Lists some <em>S3</em> <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/ErrorResponses.html">error codes</a>
 * along with their respective {@linkplain HttpResponseStatus HTTP response codes}.
 */
public enum S3ErrorCode {
    InvalidRequest(HttpResponseStatus.BAD_REQUEST),
    NoSuchBucketPolicy(HttpResponseStatus.NOT_FOUND),
    NoSuchLifecycleConfiguration(HttpResponseStatus.NOT_FOUND);

    private final HttpResponseStatus httpStatusCode;

    S3ErrorCode(HttpResponseStatus httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public HttpResponseStatus getHttpStatusCode() {
        return httpStatusCode;
    }
}
