package io.datawire.loomctl

import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.codec.binary.Hex
import org.zeroturnaround.zip.ZipUtil
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest

private val http = OkHttpClient()

data class Tool(
    val name: String,
    val url: String,
    val checksum: String,
    val afterDownload: (Path) -> Path = { it }) {

  fun download(outputRoot: Path) {
    val downloadTo = outputRoot.resolve(name)
    if (!(Files.isExecutable(downloadTo) && checksumMatches(downloadTo))) {
      println("Downloading tool '$name' from '$url' into '$downloadTo'")
      val req = Request.Builder().url(url).build()
      val res = http.newCall(req).execute()

      res.body()
          ?.apply { Files.write(downloadTo, bytes()) }
          ?: throw RuntimeException("Download '$name' failed from '$url'")
    }

    afterDownload(downloadTo)
  }

  private fun checksumMatches(path: Path): Boolean {
    val bytes = Files.readAllBytes(path)
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    val computed = Hex.encodeHexString(digest)
    return computed == checksum
  }
}

fun chmod(path: Path): Path {
  return Files.setPosixFilePermissions(path, setOf(
      PosixFilePermission.OWNER_READ,
      PosixFilePermission.OWNER_WRITE,
      PosixFilePermission.OWNER_EXECUTE,
      PosixFilePermission.GROUP_READ,
      PosixFilePermission.GROUP_EXECUTE,
      PosixFilePermission.OTHERS_READ,
      PosixFilePermission.OTHERS_EXECUTE
  ))
}

fun unzip(path: Path): Path {
  ZipUtil.unpack(path.toFile(), path.parent.toFile());
  return path.parent.resolve("terraform")
}

fun downloadTools(toolPath: Path) {
  Files.createDirectories(toolPath)

  listOf(
      Tool(
          "kops",
          "https://github.com/kubernetes/kops/releases/download/1.6.0/kops-linux-amd64",
          "91058b40241e4b9f1c94a85936626dd69eea604f448ad3164d283d9bda94b29a",
          ::chmod
      ),

      Tool(
          "kubectl",
          "https://storage.googleapis.com/kubernetes-release/release/v1.6.0/bin/linux/amd64/kubectl",
          "c488d77cd980ca7dae03bc684e19bd6a329962e32ed7a1bc9c4d560ed433399a",
          ::chmod
      ),

      Tool(
          "terraform.zip",
          "https://releases.hashicorp.com/terraform/0.9.6/terraform_0.9.6_linux_amd64.zip",
          "7ec24a5d57da6ef7bdb5a3003791a4368489b32fa93be800655ccef0eceaf1ba",
          { chmod(unzip(it)) }
      )

  ).forEach { it.download(toolPath) }
}
