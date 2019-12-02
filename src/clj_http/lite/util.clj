(ns clj-http.lite.util
  "Helper functions for the HTTP client."
  (:require [clojure.java.io :as io])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream InputStream)
           (java.net URLEncoder URLDecoder)
           (java.util.zip InflaterInputStream DeflaterInputStream
                          GZIPInputStream GZIPOutputStream)))

(defn utf8-bytes
  "Returns the UTF-8 bytes corresponding to the given string."
  [#^String s]
  (.getBytes s "UTF-8"))

(defn utf8-string
  "Returns the String corresponding to the UTF-8 decoding of the given bytes."
  [#^"[B" b]
  (String. b "UTF-8"))

(defn url-decode
  "Returns the form-url-decoded version of the given string, using either a
  specified encoding or UTF-8 by default."
  [^String encoded & [encoding]]
  (let [^String encoding (or encoding "UTF-8")]
    (URLDecoder/decode encoded encoding)))

(defn url-encode
  "Returns an UTF-8 URL encoded version of the given string."
  [^String unencoded]
  (URLEncoder/encode unencoded "UTF-8"))

(defmacro base64-encode
  "Encode an array of bytes into a base64 encoded string."
  [unencoded]
  (if (try (import 'javax.xml.bind.DatatypeConverter)
           (catch ClassNotFoundException _))
    `(javax.xml.bind.DatatypeConverter/printBase64Binary ~unencoded)
    (do
      (import 'java.util.Base64)
      `(.encodeToString (java.util.Base64/getEncoder) ~unencoded))))

(defn to-byte-array
  "Returns a byte array for the InputStream provided."
  [is]
  (let [chunk-size 8192
        baos (ByteArrayOutputStream.)
        buffer (byte-array chunk-size)]
    (loop [len (.read ^InputStream is buffer 0 chunk-size)]
      (when (not= -1 len)
        (.write baos buffer 0 len)
        (recur (.read ^InputStream is buffer 0 chunk-size))))
    (.toByteArray baos)))


(defn gunzip
  "Returns a gunzip'd version of the given byte array."
  [b]
  (when b
    (if (instance? InputStream b)
      (GZIPInputStream. b)
      (to-byte-array (GZIPInputStream. (ByteArrayInputStream. b))))))

(defn gzip
  "Returns a gzip'd version of the given byte array."
  [b]
  (when b
    (let [baos (ByteArrayOutputStream.)
          gos  (GZIPOutputStream. baos)]
      (io/copy (ByteArrayInputStream. b) gos)
      (.close gos)
      (.toByteArray baos))))

(defn inflate
  "Returns a zlib inflate'd version of the given byte array."
  [b]
  (when b
    (to-byte-array (InflaterInputStream. (ByteArrayInputStream. b)))))

(defn deflate
  "Returns a deflate'd version of the given byte array."
  [b]
  (when b
    (to-byte-array (DeflaterInputStream. (ByteArrayInputStream. b)))))
