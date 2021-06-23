/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package ninja;

import com.google.common.collect.Maps;
import sirius.kernel.commons.Strings;
import sirius.kernel.health.Exceptions;
import sirius.kernel.nls.NLS;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;

/**
 * Represents a stored object.
 */
public class StoredObject {

    private final File file;

    private final String key;

    private final String encodedKey;

    /**
     * Creates a new object from the given file.
     *
     * @param file the contents of the object.
     */
    public StoredObject(File folder, File file) {
        this.file = file;
        this.encodedKey = encodeKey(getFullName(folder, file));
        this.key = decodeKey(this.encodedKey);

        if (!Strings.areEqual(this.encodedKey, encodeKey(this.key))) {
            throw Exceptions.createHandled()
                            .withSystemErrorMessage("File name \"%s\" is not properly encoded.", key)
                            .handle();
        }
    }

    public static StoredObject inFolder(File folder, String key) {
        return new StoredObject(folder, new File(folder, key));
    }

    public static String getFullName(File folder, File path) {
        if (folder != null) {
            String folderAbsolutePath = folder.getAbsolutePath();
            String absolutePath = path.getAbsolutePath();

            if (absolutePath.startsWith(folderAbsolutePath)) {
                return absolutePath.substring(folderAbsolutePath.length() + 1);
            }
        }
        return path.toPath().getFileName().toString();
    }

    /**
     * Encodes an object key for use as file name.
     *
     * @param key the key to encode
     * @return the encoded key
     */
    public static String encodeKey(String key) {
        return Strings.urlEncode(key).replace("+", "%20");
    }

    /**
     * Decodes an encoded object key.
     *
     * @param key the key to decode
     * @return the decoded key
     */
    public static String decodeKey(String key) {
        try {
            return URLDecoder.decode(key, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw Exceptions.handle(Storage.LOG, e);
        }
    }

    /**
     * Returns the name of the object.
     *
     * @return the name of the object
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the encoded name of the object.
     *
     * @return the encoded name of the object
     */
    public String getEncodedKey() {
        return encodedKey;
    }

    /**
     * Returns the size of the object.
     *
     * @return a string representation of the byte-size of the object
     */
    public String getSize() {
        return NLS.formatSize(getSizeBytes());
    }

    /**
     * Returns the size of the object in bytes.
     *
     * @return the byte-size of the object
     */
    public long getSizeBytes() {
        return file.length();
    }

    /**
     * Returns the object's date of last modification.
     *
     * @return a string representation of the last modification date
     */
    public String getLastModified() {
        return NLS.toUserString(getLastModifiedInstant());
    }

    /**
     * Returns the object's date of last modification.
     *
     * @return the last modification date as {@link Instant}
     */
    public Instant getLastModifiedInstant() {
        return Instant.ofEpochMilli(file.lastModified());
    }

    /**
     * Deletes the object.
     */
    public void delete() {
        if (!file.delete()) {
            Storage.LOG.WARN("Failed to delete data file for object %s (%s).", getKey(), file.getAbsolutePath());
        }
        if (!getPropertiesFile().delete()) {
            Storage.LOG.WARN("Failed to delete properties file for object %s (%s).",
                             getKey(),
                             getPropertiesFile().getAbsolutePath());
        }
    }

    /**
     * Returns the underlying file.
     *
     * @return the underlying file containing the stored contents
     */
    public File getFile() {
        return file;
    }

    /**
     * Determines if the object exists.
     *
     * @return <b>true</b> if the object exists, <b>false</b> else
     */
    public boolean exists() {
        return file.exists();
    }

    /**
     * Returns the file used to store the properties and meta headers.
     *
     * @return the underlying file used to store the meta infos
     */
    public File getPropertiesFile() {
        return new File(file.getParentFile(), "$" + file.getName() + ".properties");
    }

    /**
     * Returns all meta information stored along with the object.
     * <p>
     * This is the <tt>Content-MD5</tt>, <tt>Content-Type</tt> and any <tt>x-amz-meta-*</tt> header.
     * <p>
     * Internally, a {@link Properties} file is loaded from disk and converted to a {@link Map}.
     *
     * @return a set of name value pairs representing all properties stored for this object, or an empty set if no
     * properties could be read
     */
    public Map<String, String> getProperties() {
        // read properties object from disk
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(getPropertiesFile())) {
            props.load(in);
        } catch (IOException e) {
            Exceptions.ignore(e);
        }

        // convert the properties object to a string-to-string-map
        Map<String, String> map = Maps.newTreeMap();
        props.forEach((key, value) -> map.put(String.valueOf(key), String.valueOf(value)));
        return map;
    }

    /**
     * Stores the given meta information for this object.
     * <p>
     * Internally, the map is transformed to a {@link Properties} object and stored to disk.
     *
     * @param properties the properties to store
     * @throws IOException in case of an IO error
     */
    public void setProperties(Map<String, String> properties) throws IOException {
        Properties props = new Properties();
        properties.forEach(props::setProperty);
        try (FileOutputStream out = new FileOutputStream(getPropertiesFile())) {
            props.store(out, "");
        }
    }

    /**
     * Checks whether the given string is valid for use as object key.
     * <p>
     * Currently, the key only must not be empty. All UTF-8 characters are valid, but names should be restricted to a
     * subset. See the <a href="https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html">official naming
     * recommendations</a>.
     *
     * @param key the key to check
     * @return <b>true</b> if the key is valid as object key, <b>false</b> else
     */
    public static boolean isValidKey(@Nullable String key) {
        return Strings.isFilled(key);
    }
}
