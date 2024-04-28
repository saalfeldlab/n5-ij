package org.janelia.saalfeldlab.n5.ui;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

/**
 * <p>
 * Extension of {@code JFormattedTextField} which solves some of the usability
 * issues
 * </p>
 *
 * <p>
 * from
 * https://stackoverflow.com/questions/1313390/is-there-any-way-to-accept-only-numeric-values-in-a-jtextfield?answertab=scoredesc#tab-top
 * </p>
 */
public class ImprovedFormattedTextField extends JFormattedTextField {

	private static final long serialVersionUID = 6986337989217402465L;

	private static final Color ERROR_BACKGROUND_COLOR = new Color(255, 215, 215);

	private static final Color ERROR_FOREGROUND_COLOR = null;

	private Color fBackground, fForeground;

	private Runnable updateCallback;

	private boolean runCallback;

	public ImprovedFormattedTextField(AbstractFormatter formatter) {

		super(formatter);

//		setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
		setFocusLostBehavior(JFormattedTextField.PERSIST);
//		setFocusLostBehavior(JFormattedTextField.COMMIT);
//		setFocusLostBehavior(JFormattedTextField.REVERT);

		updateBackgroundOnEachUpdate();

		// improve the caret behavior
		// see also
		// http://tips4java.wordpress.com/2010/02/21/formatted-text-field-tips/
		addFocusListener(new MousePositionCorrectorListener());
		addFocusListener(new ContainerTextUpdateOnFocus(this));
		runCallback = true;
	}

	/**
	 * Create a new {@code ImprovedFormattedTextField} instance which will use
	 * {@code aFormat} for the validation of the user input. The field will be
	 * initialized with {@code aValue}.
	 *
	 * @param formatter
	 *            The formatter. May not be {@code null}
	 * @param aValue
	 *            The initial value
	 */
	public ImprovedFormattedTextField(AbstractFormatter formatter, Object aValue) {

		this(formatter);
		try {
			setValue(new URI(""));
		} catch (URISyntaxException e) { 
			e.printStackTrace();
		}
	}

	public void setCallback(final Runnable updateCallback) {

		this.updateCallback = updateCallback;
	}

	private void updateBackgroundOnEachUpdate() {

		getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {

				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {

				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {

				update();
			}

			public void update() {

				updateBackground();
				if (runCallback && updateCallback != null)
					updateCallback.run();
			}
		});
	}

	/**
	 * Update the background color depending on the valid state of the current
	 * input. This provides visual feedback to the user
	 */
	private void updateBackground() {

		final boolean valid = validContent();
		if (ERROR_BACKGROUND_COLOR != null) {
			setBackground(valid ? fBackground : ERROR_BACKGROUND_COLOR);
		}
		if (ERROR_FOREGROUND_COLOR != null) {
			setForeground(valid ? fForeground : ERROR_FOREGROUND_COLOR);
		}
	}

	@Override
	public void updateUI() {

		super.updateUI();
		fBackground = getBackground();
		fForeground = getForeground();
	}

	private boolean validContent() {

		final AbstractFormatter formatter = getFormatter();
		if (formatter != null) {
			try {
				formatter.stringToValue(getText());
				return true;
			} catch (final ParseException e) {
				return false;
			}
		}
		return true;
	}


	public void setValue(Object value, boolean callback, boolean validate) {

		boolean validValue = true;
		// before setting the value, parse it by using the format
		try {
			final AbstractFormatter formatter = getFormatter();
			if (formatter != null && validate ) {
				formatter.stringToValue(getText());
			}
		} catch (final ParseException e) {
			validValue = false;
			updateBackground();
		}
		// only set the value when valid
		if (validValue) {
			final int old_caret_position = getCaretPosition();

			final boolean before = runCallback;
			runCallback = callback;
			super.setValue(value);
			runCallback = before;

			setCaretPosition(Math.min(old_caret_position, getText().length()));
		}
	}

	public void setValue(Object value, boolean callback) {

		setValue(value, callback, true);
	}

	public void setValueNoCallback(Object value) {

		setValue(value, false);
	}

	@Override
	public void setValue(Object value) {

		setValue(value, true);
	}

	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {

		// do not let the formatted text field consume the enters. This allows
		// to trigger an OK button by
		// pressing enter from within the formatted text field
		if (validContent()) {
			return super.processKeyBinding(ks, e, condition, pressed) && ks != KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		} else {
			return super.processKeyBinding(ks, e, condition, pressed);
		}
	}

	private static class MousePositionCorrectorListener extends FocusAdapter {

		@Override
		public void focusGained(FocusEvent e) {

			/*
			 * After a formatted text field gains focus, it replaces its text
			 * with its current value, formatted appropriately of course. It
			 * does this after any focus listeners are notified. We want to make
			 * sure that the caret is placed in the correct position rather than
			 * the dumb default that is before the 1st character !
			 */
			final JTextField field = (JTextField)e.getSource();
			final int dot = field.getCaret().getDot();
			final int mark = field.getCaret().getMark();
			if (field.isEnabled() && field.isEditable()) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {

						// Only set the caret if the textfield hasn't got a
						// selection on it
						if (dot == mark) {
							field.getCaret().setDot(dot);
						}
					}
				});
			}
		}
	}
	
	private static class ContainerTextUpdateOnFocus extends FocusAdapter {
		
		private final ImprovedFormattedTextField field;

		public ContainerTextUpdateOnFocus(ImprovedFormattedTextField field) {

			this.field = field;
		}

		@Override
		public void focusLost(FocusEvent e) {

			final AbstractFormatter formatter = field.getFormatter();
			if (formatter != null) {
				try {
					Object result = formatter.stringToValue((String)field.getText());
					field.setValue((URI)result, false, false); // no callback, no validation
				} catch (ParseException ignore) {
					ignore.printStackTrace();
				}
			}

		}
	}

}
