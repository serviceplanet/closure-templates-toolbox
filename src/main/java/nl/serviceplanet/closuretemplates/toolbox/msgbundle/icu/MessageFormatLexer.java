/*
 * Copyright © 2024 Service Planet Rotterdam B.V. (it@ask.serviceplanet.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.serviceplanet.closuretemplates.toolbox.msgbundle.icu;

import com.google.common.collect.ImmutableList;
import nl.serviceplanet.closuretemplates.toolbox.msgbundle.util.TextUtil;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple lexer for the ICU message format.
 *
 * @author Jasper Siepkes <siepkes@serviceplanet.nl>
 */
public final class MessageFormatLexer {
	/**
	 * The message in ICU message format we need to parse.
	 */
	private final String formattedMessage;
	private int position;

	public MessageFormatLexer(String formattedMessage) {
		this.formattedMessage = formattedMessage;
		this.position = 0;
	}


	public ImmutableList<MessageFormatToken> tokenize() {
		checkNotNull(formattedMessage);

		ImmutableList.Builder<MessageFormatToken> tokens = ImmutableList.builder();

		while (position < formattedMessage.length()) {
			char current = formattedMessage.charAt(position);
			if (current == '{') {
				// {...,select,.............}
				// {...,plural,.............}

				String expr = extractExpression();
				String keyword = TextUtil.between(expr, ",", ",", "unknown");

				MessageFormatTokenType type = switch (keyword) {
					case "select" -> MessageFormatTokenType.SELECT;
					case "plural" -> MessageFormatTokenType.PLURAL;
					default -> MessageFormatTokenType.PLACEHOLDER;
				};

				tokens.add(new MessageFormatToken(type, expr));
			} else {
				tokens.add(new MessageFormatToken(MessageFormatTokenType.TEXT, extractText()));
			}
		}
		tokens.add(new MessageFormatToken(MessageFormatTokenType.EOF, ""));

		return tokens.build();
	}

	private String extractText() {
		int start = position;
		while (position < formattedMessage.length() && formattedMessage.charAt(position) != '{') {
			if (formattedMessage.charAt(position) == '}') {
				throw new IllegalStateException("Unexpected end-of-expression in text-block: '" + formattedMessage.substring(start) + "'");
			}
			position++;
		}
		return formattedMessage.substring(start, position);
	}

	private String extractExpression() {
		int start = position;
		int braceCount = 1;
		position++; // Skip the opening brace
		while (position < formattedMessage.length() && braceCount > 0) {
			if (formattedMessage.charAt(position) == '{') {
				braceCount++;
			} else if (formattedMessage.charAt(position) == '}') {
				braceCount--;
			}
			position++;
		}
		if (braceCount != 0) {
			throw new IllegalStateException("Unclosed expression");
		}
		return formattedMessage.substring(start, position);
	}
}
