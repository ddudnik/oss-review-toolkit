/*
 * Copyright (C) 2019 Bosch Software Innovations GmbH
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.ort.analyzer

import com.here.ort.clearlydefined.ClearlyDefinedService
import com.here.ort.clearlydefined.ClearlyDefinedService.Provider
import com.here.ort.clearlydefined.ClearlyDefinedService.Server
import com.here.ort.clearlydefined.ClearlyDefinedService.SourceLocation
import com.here.ort.clearlydefined.ClearlyDefinedService.Type
import com.here.ort.model.Identifier
import com.here.ort.model.Hash
import com.here.ort.model.PackageCuration
import com.here.ort.model.PackageCurationData
import com.here.ort.model.RemoteArtifact
import com.here.ort.model.VcsInfoCuration
import com.here.ort.model.VcsType

class ClearlyDefinedPackageCurationProvider(server: Server = Server.PRODUCTION) : PackageCurationProvider {
    private val service = ClearlyDefinedService.create(server)

    /**
     * Map an ORT [Package id][pkgId] to a ClearlyDefined type and provider. Note that a Package's type in ORT currently
     * implies a default provider.
     */
    private fun mapIdToTypeAndProvider(pkgId: Identifier): Pair<Type, Provider> {
        return when (pkgId.type) {
            "Bower" -> Type.GIT to Provider.GITHUB
            "Bundler" -> Type.GEM to Provider.RUBYGEMS
            "Cargo" -> Type.CRATE to Provider.CRATES_IO
            "CocoaPods" -> Type.POD to Provider.COCOAPODS
            "nuget" -> Type.NUGET to Provider.NUGET
            "GoDep" -> Type.GIT to Provider.GITHUB
            "Maven" -> Type.MAVEN to Provider.MAVEN_CENTRAL
            "NPM" -> Type.NPM to Provider.NPM_JS
            "PhpComposer" -> Type.COMPOSER to Provider.PACKAGIST
            "PyPI" -> Type.PYPI to Provider.PYPI
            "Pub" -> Type.GIT to Provider.GITHUB
            else -> throw IllegalArgumentException("Unknown mapping of ORT type '${pkgId.type}' to ClearlyDefined.")
        }
    }

    /**
     * Map a ClearlyDefined [sourceLocation] to either an ORT [VcsInfoCuration] or [RemoteArtifact].
     */
    private fun mapSourceLocationToArtifactOrVcs(sourceLocation: SourceLocation?): Any? {
        if (sourceLocation == null) return null

        return when (sourceLocation.type) {
            Type.GIT -> {
                VcsInfoCuration(
                    type = VcsType.GIT,
                    url = sourceLocation.url,
                    revision = sourceLocation.revision,
                    path = sourceLocation.path
                )
            }

            else -> {
                val url = sourceLocation.url ?: run {
                    when (sourceLocation.provider) {
                        // TODO: Implement provider-specific mapping of coordinates to URLs.
                        else -> ""
                    }
                }

                RemoteArtifact(
                    url = url,
                    hash = Hash.NONE
                )
            }
        }
    }

    override fun getCurationsFor(pkgId: Identifier): List<PackageCuration> {
        val namespace = pkgId.namespace.takeUnless { it.isEmpty() } ?: "-"
        val (type, provider) = mapIdToTypeAndProvider(pkgId)
        val curationCall = service.getCuration(type, provider, namespace, pkgId.name, pkgId.version)

        val curation = curationCall.execute().body() ?: return emptyList()

        val sourceLocation = mapSourceLocationToArtifactOrVcs(curation.described?.sourceLocation)
        val pkgCuration = PackageCuration(
            id = pkgId,
            data = PackageCurationData(
                declaredLicenses = curation.licensed?.declared?.let { sortedSetOf(it) },
                homepageUrl = curation.described?.projectWebsite?.toString(),
                sourceArtifact = sourceLocation as? RemoteArtifact,
                vcs = sourceLocation as? VcsInfoCuration,
                comment = "Provided by ClearlyDefined."
            )
        )

        return listOf(pkgCuration)
    }
}
