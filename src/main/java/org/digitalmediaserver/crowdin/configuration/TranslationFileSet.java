/*
 * Crowdin Maven Plugin, an Apache Maven plugin for synchronizing translation
 * files using the crowdin.com API.
 * Copyright (C) 2018 Digital Media Server developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.digitalmediaserver.crowdin.configuration;

import static org.digitalmediaserver.crowdin.AbstractCrowdinMojo.isBlank;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.digitalmediaserver.crowdin.tool.CrowdinFileSystem;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * A {@link Mojo} configuration class describing a set of translation files.
 *
 * @author Nadahar
 */
@SuppressFBWarnings("UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
public class TranslationFileSet extends AbstractFileSet {

	/**
	 * The folder where the language files are located.
	 *
	 * @parameter
	 * @required
	 */
	protected File languageFilesFolder;

	/**
	 * The path from the Crowdin (branch) root folder to the files of this
	 * {@link TranslationFileSet}. Only specify it the files are located in a
	 * subfolder at Crowdin.
	 *
	 * @parameter
	 */
	protected String crowdinPath;

	/**
	 * The base language file that should be uploaded to Crowdin.
	 *
	 * @parameter
	 * @required
	 */
	protected String baseFileName;

	/**
	 * The base language file that should be uploaded to Crowdin.
	 *
	 * @parameter
	 * @required
	 */
	protected String uploadedFileName;

	/**
	 * The title as it should appear to translators at Crowdin.
	 *
	 * @parameter
	 */
	protected String title;

	/**
	 * The comment character or character combination to use for comment lines
	 * when exporting files from Crowdin if {@code addComment} is {@code true}.
	 *
	 * @parameter default-value="#"
	 */
	protected String commentTag;

	/**
	 * The value to use in the {@code "Resulting file name when exported"}
	 * setting at Crowdin.
	 * <p>
	 * The following variables are available:
	 * <ul>
	 * <li><b>%language%</b> &ndash; Language name (e.g. Ukrainian)</li>
	 * <li><b>%two_letters_code%</b> &ndash; Language code {@code ISO 639-1}
	 * (i.e. uk)</li>
	 * <li><b>%three_letters_code%</b> &ndash; Language code {@code ISO 639-2/T}
	 * (i.e. ukr)</li>
	 * <li><b>%locale%</b> &ndash; Locale (like uk-UA)</li>
	 * <li><b>%locale_with_underscore%</b> &ndash; Locale (i.e. uk_UA)</li>
	 * <li><b>%android_code%</b> &ndash; Android Locale identifier used to name
	 * "values-" directories</li>
	 * <li><b>%osx_code%</b> &ndash; macOS Locale identifier used to name
	 * ".lproj" directories</li>
	 * <li><b>%osx_locale%</b> &ndash; macOS Locale used to name translated
	 * resources (i.e. uk, zh_Hans)</li>
	 * <li><b>%original_file_name%</b> &ndash; Original file name</li>
	 * <li><b>%file_name%</b> &ndash; File name without extension</li>
	 * <li><b>%file_extension%</b> &ndash; Original file extension</li>
	 * <li><b>%original_path%</b> &ndash; Use parent folders' names in your
	 * project to build the file path in the resulting archive</li>
	 * </ul>
	 *
	 * @parameter
	 * @required
	 */
	protected String fileNameWhenExported;

	/**
	 * The file path relative to {@link #languageFilesFolder} to use when
	 * deploying the translation files. If left blank,
	 * {@link #fileNameWhenExported} will be used.
	 * <p>
	 * The following variables are available:
	 * <ul>
	 * <li><b>%crowdin_code%</b> &ndash; Crowdin language code (i.e en-GB or da)
	 * </li>
	 * <li><b>%crowdin_code_with_underscore%</b> &ndash; Crowdin language code
	 * with underscore (i.e en_GB or da)</li>
	 * <li><b>%shortest_iso639_code%</b> &ndash; The shortest {@code ISO 639}
	 * language code (i.e en or ceb)</li>
	 * <li><b>%language%</b> &ndash; Language name (e.g. Ukrainian)</li>
	 * <li><b>%two_letters_code%</b> &ndash; Language code {@code ISO 639-1}
	 * (i.e. uk)</li>
	 * <li><b>%three_letters_code%</b> &ndash; Language code {@code ISO 639-2/T}
	 * (i.e. ukr)</li>
	 * <li><b>%locale%</b> &ndash; Locale (like uk-UA) (*)</li>
	 * <li><b>%locale_with_underscore%</b> &ndash; Locale (i.e. uk_UA) (*)</li>
	 * <li><b>%android_code%</b> &ndash; Android Locale identifier used to name
	 * "values-" directories (*)</li>
	 * <li><b>%osx_code%</b> &ndash; macOS Locale identifier used to name
	 * ".lproj" directories (*)</li>
	 * <li><b>%osx_locale%</b> &ndash; macOS Locale used to name translated
	 * resources (i.e. uk, zh_Hans) (*)</li>
	 * <li><b>%original_file_name%</b> &ndash; Original file name (*)</li>
	 * <li><b>%file_name%</b> &ndash; File name without extension (*)</li>
	 * <li><b>%file_extension%</b> &ndash; Original file extension (*)</li>
	 * <li><b>%original_path%</b> &ndash; Use parent folders' names in your
	 * project to build the file path in the resulting archive (*)</li>
	 * </ul>
	 * (*) The variable must also be used in {@link #fileNameWhenExported} to be
	 * supported.
	 *
	 * @parameter
	 */
	@Nullable
	protected String targetFileName;

	/**
	 * The {@code escape_quotes} API parameter to use. Valid values are:
	 * <ul>
	 * <li>0 — Do not escape single quote</li>
	 * <li>1 — Escape single quote by another single quote</li>
	 * <li>2 — Escape single quote by backslash</li>
	 * <li>3 — Escape single quote by another single quote only in strings
	 * containing variables (<code>{0}</code>)</li>
	 * </ul>
	 *
	 * @parameter
	 */
	@Nullable
	protected Integer escapeQuotes;

	/**
	 * The update behavior for updates string when pushing. Valid values are:
	 * <ul>
	 * <li>delete_translations — Delete translations of changed strings</li>
	 * <li>update_as_unapproved — Preserve translations of changed strings but
	 * remove validations of those translations if they exist</li>
	 * <li>update_without_changes — Preserve translations and validations of
	 * changed strings</li>
	 * </ul>
	 *
	 * @parameter default-value="delete_translations"
	 */
	protected UpdateOption updateOption;

	/**
	 * Paths to include using a basic filter where {@code ?} and {@code *} are
	 * wildcards and the rest are literals. If one or more includes are
	 * configured the file set becomes a white-list where anything not included
	 * is excluded.
	 *
	 * @parameter
	 */
	@Nullable
	protected List<String> includes;

	/**
	 * Paths to exclude using a basic filter where {@code ?} and {@code *} are
	 * wildcards and the rest are literals.
	 *
	 * @parameter
	 */
	@Nullable
	protected List<String> excludes;

	/**
	 * @return The comment tag.
	 */
	public String getCommentTag() {
		return commentTag;
	}

	/**
	 * Sets the comment tag.
	 *
	 * @param commentTag the comment tag to set.
	 */
	public void setCommentTag(String commentTag) {
		this.commentTag = commentTag;
	}

	/**
	 * @return The language files folder.
	 */
	public File getLanguageFilesFolder() {
		return languageFilesFolder;
	}

	/**
	 * @return The path from the Crowdin (branch) root folder to the files of
	 *         this {@link TranslationFileSet} or {@code null} it the files are
	 *         in the root of the Crowdin (branch) file structure.
	 */
	public String getCrowdinPath() {
		return crowdinPath;
	}

	/**
	 * @return The base filename.
	 */
	public String getBaseFileName() {
		return baseFileName;
	}

	/**
	 * @return The uploaded filename.
	 */
	public String getUploadedFileName() {
		return uploadedFileName;
	}


	/**
	 * @return The title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return The "Resulting file name when exported" to use on Crowdin.
	 */
	@Nonnull
	public String getFileNameWhenExported() {
		return fileNameWhenExported;
	}

	/**
	 * @return The file path relative to {@link #languageFilesFolder} to use
	 *         when deploying translation files. If left blank,
	 *         {@link #fileNameWhenExported} will be used.
	 */
	@Nullable
	public String getTargetFileName() {
		return targetFileName;
	}

	/**
	 * @return The {@code escape_quotes} API parameter.
	 */
	@Nullable
	public Integer getEscapeQuotes() {
		return escapeQuotes;
	}

	/**
	 * @return The {@link UpdateOption}.
	 */
	public UpdateOption getUpdateOption() {
		return updateOption;
	}

	/**
	 * @return The {@link List} of string patterns for paths to include. The
	 *         patterns use a basic filter where {@code ?} and {@code *} are
	 *         wildcards and the rest are literals. If one or more inclusions
	 *         are configured the file set becomes a white-list where anything
	 *         not included is excluded.
	 */
	@Nullable
	public List<String> getIncludes() {
		return includes;
	}

	/**
	 * @return The {@link List} of string patterns for paths to exclude. The
	 *         patterns use a basic filter where {@code ?} and {@code *} are
	 *         wildcards and the rest are literals.
	 */
	@Nullable
	public List<String> getExcludes() {
		return excludes;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": \"" + title + "\"";
	}

	@Override
	protected void initializeInstance() throws MojoExecutionException {

		// Title and base file name.
		if (baseFileName != null) {
			baseFileName = CrowdinFileSystem.formatPath(baseFileName, false);
		}
		if (isBlank(title)) {
			title = baseFileName;
		}
		if (isBlank(baseFileName)) {
			if (isBlank(title)) {
				throw new MojoExecutionException("\"baseFileName\" isn't defined for translation fileset");
			}
			throw new MojoExecutionException(
				"\"baseFileName\" isn't defined for translation fileset \"" + title + "\""
			);
		}

		// Folder
		if (languageFilesFolder == null) {
			throw new MojoExecutionException(
				"\"languageFilesFolder\" isn't defined for translation fileset \"" + title + "\""
			);
		}
		if (!languageFilesFolder.isDirectory()) {
			throw new MojoExecutionException(
				"The specified folder \"" + languageFilesFolder.getAbsolutePath() +
				"\" for translation fileset \"" + title + "\" either doesn't exist or isn't a folder"
			);
		}

		// Crowdin path
		if (crowdinPath != null) {
			crowdinPath = CrowdinFileSystem.formatPath(crowdinPath, false);
		}

		// File type
		if (type == null) {
			int dot = baseFileName.lastIndexOf('.');
			if (dot > 0 && dot < baseFileName.length() - 1) {
				String extension = baseFileName.substring(dot + 1);
				for (FileType fileType : FileType.values()) {
					if (fileType.hasExtension(extension)) {
						type = fileType;
						break;
					}
				}
			}
			if (type == null) {
				type = FileType.auto;
			}
		}

		// CharSet
		switch (type) {
			// Set charset from type when it is defined
			case properties:
				if (isBlank(encoding)) {
					charset = StandardCharsets.ISO_8859_1;
					encoding = charset.name();
				} else {
					charset = Charset.forName(encoding);
				}
				if (sortLines == null) {
					sortLines = Boolean.TRUE;
				}
				if (escapeUnicode == null) {
					escapeUnicode = Boolean.TRUE;
				}
				break;
			case xml:
				charset = StandardCharsets.UTF_8;
				encoding = charset.name();
				break;
			case android:
			case auto:
			case chrome:
			case csv:
			case dklang:
			case docx:
			case dtd:
			case flex:
			case flsnp:
			case fm_html:
			case fm_md:
			case gettext:
			case haml:
			case html:
			case ini:
			case joomla:
			case json:
			case macosx:
			case md:
			case mediawiki:
			case nsh:
			case php:
			case qtts:
			case rc:
			case resjson:
			case resw:
			case resx:
			case sbv:
			case srt:
			case txt:
			case vtt:
			case wxl:
			case xliff:
			case yaml:
			default:
				// Parse the encoding parameter
				if (!isBlank(encoding)) {
					charset = Charset.forName(encoding);
					break;
				}
				// Default to UTF-8
				charset = StandardCharsets.UTF_8;
				encoding = charset.name();
				break;
		}


		// Sort lines
		if (sortLines == null) {
			sortLines = Boolean.FALSE;
		}

		// Add comment
		if (addComment == null) {
			addComment = Boolean.TRUE;
		}

		// Comment tag
		if (commentTag == null) {
			commentTag = "#";
		}

		// Filename when exported
		if (isBlank(fileNameWhenExported)) {
			throw new MojoExecutionException(
				"\"fileNameWhenExported\" isn't defined for translation fileset \"" + title + "\""
			);
		}

		// Escape single quotes
		if (escapeQuotes != null && (escapeQuotes.intValue() < 0 || escapeQuotes.intValue() > 3)) {
			throw new MojoExecutionException(
				"Invalid \"escapeQuotes\" value " + escapeQuotes.intValue() + " for translation fileset \"" + title + "\""
			);
		}

		// Target filename
		if (targetFileName != null) {
			targetFileName = CrowdinFileSystem.formatPath(targetFileName, false);
		}

		super.initializeInstance();
	}
}
