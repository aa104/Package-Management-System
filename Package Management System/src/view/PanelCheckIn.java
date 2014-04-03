package view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import javax.swing.JComboBox;

public class PanelCheckIn extends JPanel {

	private static final long serialVersionUID = -5330361979358721465L;
	
	private JComboBox<String> comboBoxStudentName;
	private JTextField textField;

	/**
	 * Create the panel.
	 */
	public PanelCheckIn(JFrame frame, IViewToModelAdaptor modelAdaptor) {

		setLayout(new FormLayout(new ColumnSpec[] {
				ColumnSpec.decode("default:grow"),
				FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("pref:grow"),},
			new RowSpec[] {
				RowSpec.decode("36px:grow"),
				FormFactory.DEFAULT_ROWSPEC,
				RowSpec.decode("20px"),
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),}));
		
		JLabel lblStudent = new JLabel("Student:");
		add(lblStudent, "3, 2, left, bottom");
		
		comboBoxStudentName = new JComboBox<String>();
		comboBoxStudentName.setEditable(true);
		add(comboBoxStudentName, "3, 3, fill, top");
		
		JButton btnConfirmCheckIn = new JButton("Confirm");
		btnConfirmCheckIn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
			}
		});
		
		JLabel lblNewLabel = new JLabel("Comment:");
		add(lblNewLabel, "3, 5, left, bottom");
		
		textField = new JTextField();
		add(textField, "3, 6, fill, default");
		textField.setColumns(20);
		add(btnConfirmCheckIn, "3, 8, center, default");
		
	}

}
