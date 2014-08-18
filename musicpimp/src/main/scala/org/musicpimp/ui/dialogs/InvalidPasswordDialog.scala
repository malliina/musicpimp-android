package org.musicpimp.ui.dialogs

import com.mle.andro.ui.dialogs.DefaultDialog
import org.musicpimp.R

/**
 *
 * @author mle
 */
class InvalidPasswordDialog extends DefaultDialog(R.string.invalid_pass_add_manually, negativeText = None)

class GenericErrorAddManuallyDialog extends DefaultDialog(R.string.generic_error_configure_manually, negativeText = None)

class NoCameraDialog extends DefaultDialog(message = R.string.camera_required_none_found, negativeText = None)