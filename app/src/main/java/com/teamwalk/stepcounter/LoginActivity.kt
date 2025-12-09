package com.teamwalk.stepcounter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.teamwalk.stepcounter.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val activityGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
            val notificationGranted =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
                } else {
                    true
                }

            if(!activityGranted || !notificationGranted) {
                showPermissionGuidanceDialog()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Log.d("LoginActivity", "login activity create")

        auth = FirebaseAuth.getInstance()
        checkAndRequestPermissions()

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            PermissionChecker().isPermissionDenied(this, Manifest.permission.ACTIVITY_RECOGNITION)
        ) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            PermissionChecker().isPermissionDenied(this, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Log.d("LoginActivity", "로그인 실패: ${e.message}")
                Toast.makeText(this@LoginActivity, "로그인 취소 또는 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                firebaseAuthWithGoogle(idToken)
            } catch (e: Exception) {
                Log.e("LoginActivity", "Google Id 토큰 파싱 실패", e)
            }
        } else {
            Log.e("LoginActivity", "알 수 없는 자격 증명 타입")
        }
    }

    private fun firebaseAuthWithGoogle(idtoken: String) {
        val credential = GoogleAuthProvider.getCredential(idtoken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "로그인 인증 실패: ${task.exception?.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
    }

    private fun showPermissionGuidanceDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한 필요")
            .setMessage("만보기 기능을 사용하려면 '신체 활동' 및 '알림' 권한이 모두 필요합니다. 설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    companion object {
        const val WEB_CLIENT_ID =
            "247835682968-sjrjm3i2ja99lqvrfejr1m8csmo6j8jq.apps.googleusercontent.com"
    }
}