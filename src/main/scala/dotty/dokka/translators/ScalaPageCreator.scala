package dotty.dokka

import org.jetbrains.dokka.base.translators.documentables.{DefaultPageCreator, PageContentBuilder, PageContentBuilder$DocumentableContentBuilder}
import org.jetbrains.dokka.base.signatures.SignatureProvider
import org.jetbrains.dokka.base.transformers.pages.comments.CommentsToContentConverter
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.dokka.model._
import org.jetbrains.dokka.pages._
import collection.JavaConverters._
import org.jetbrains.dokka.model.properties._
import org.jetbrains.dokka.base.transformers.documentables.CallableExtensions
import org.jetbrains.dokka.DokkaConfiguration$DokkaSourceSet
import org.jetbrains.dokka.base.resolvers.anchors._
import org.jetbrains.dokka.links._
import org.jetbrains.dokka.model.doc._
import org.jetbrains.dokka.links.DRIKt.getParent


class ScalaPageCreator(
    commentsToContentConverter: CommentsToContentConverter,
    signatureProvider: SignatureProvider,
    val logger: DokkaLogger
) extends DefaultPageCreator(commentsToContentConverter, signatureProvider, logger) {

    private val contentBuilder = ScalaPageContentBuilder(commentsToContentConverter, signatureProvider, logger)

    override def pageForModule(m: DModule): ModulePageNode = super.pageForModule(m)

    override def pageForPackage(p: DPackage): PackagePageNode = {
        val page = super.pageForPackage(p)

        val ext = Option(p.get(PackageExtension))
        val extensionPages = ext.fold(
                List.empty
            )(
                _.extensions.flatMap(_.extensions)
                .map(pageForFunction(_))
                .map(page =>
                    page.modified(
                        "extension_" + page.getName,
                        page.getChildren
                    )
                )
            )

        page.modified(
            page.getName,
            (page.getChildren.asScala ++ extensionPages).asJava
        )
    }

    override def pageForClasslike(c: DClasslike): ClasslikePageNode = c match {
            case clazz: DClass => pageForDClass(clazz)
            case other => throw UnsupportedOperationException("Only DClass classlike is supported.")
        }

    def pageForDClass(c: DClass): ClasslikePageNode = {
        val constructors = c.getConstructors

        val ext = c.get(ClasslikeExtension)

        val name = if ext.kind == dotty.dokka.Kind.Object && ext.companion.isDefined then c.getName + "$" else c.getName

        val extensionPages = ext.extensions.flatMap(_.extensions)
            .map(pageForFunction(_))
            .map(page =>
                page.modified(
                    "extension_" + page.getName,
                    page.getChildren
                )
            )
        val enumEntryPages = Option(c.get(EnumExtension)).map(_.enumEntries).collect{
            case c: DClasslike => pageForClasslike(c)
        }

        ClasslikePageNode(
            name,
            contentForClasslike(c),
            JSet(c.getDri),
            c,
            (constructors.asScala.map(pageForFunction) ++
            c.getClasslikes.asScala.map(pageForClasslike) ++
            c.getFunctions.asScala.map(pageForFunction) ++
            enumEntryPages ++ extensionPages).asJava,
            List.empty.asJava
        )

    }

    override def pageForFunction(f: DFunction) = super.pageForFunction(f)

    override def contentForModule(m: DModule) = {
        def buildBlock = (builder: ScalaPageContentBuilder#ScalaDocumentableContentBuilder) => builder
            .group(kind = ContentKind.Cover) { gbuilder => gbuilder
                .cover(m.getName)()
                .descriptionIfNotEmpty(m)
            }
            .addChildren(contentForComments(m).asScala.toSeq)
            .groupingBlock(
                "Packages",
                List("" -> m.getPackages.asScala.toList),
                kind = ContentKind.Packages,
                sourceSets = m.getSourceSets.asScala.toSet
            )(
                (bdr, elem) => bdr
            ) { (bdr, elem) => bdr
                .driLink(elem.getName, elem.getDri)
            }
            
        contentBuilder.contentForDocumentable(m, buildBlock = buildBlock)
    }

    override def contentForPackage(p: DPackage) = {
        def buildBlock = (builder: ScalaPageContentBuilder#ScalaDocumentableContentBuilder) => builder
            .group(kind = ContentKind.Cover) { gbuilder => gbuilder
                .cover(p.getName)()
                .descriptionIfNotEmpty(p)
            }
            .group(styles = Set(ContentStyle.TabbedContent)) { b => b
                .contentForScope(p)
            }
        
        contentBuilder.contentForDocumentable(p, buildBlock = buildBlock)
    }

    override def contentForClasslike(c: DClasslike) = c match {
        case d: DClass => contentForClass(d)
        case other => throw UnsupportedOperationException("Only DClass classlike is supported.")
    }

    def contentForClass(c: DClass) = {
        val ext = c.get(ClasslikeExtension)
        val co = ext.companion

        def buildBlock = (builder: ScalaPageContentBuilder#ScalaDocumentableContentBuilder) => builder
            .group(kind = ContentKind.Cover, sourceSets = c.getSourceSets.asScala.toSet) { gbdr => gbdr
                .cover(c.getName)()
                .sourceSetDependentHint(Set(c.getDri), c.getSourceSets.asScala.toSet) { sbdr => 
                    val s1 = sbdr
                        .signature(c)
                    co.fold(s1){ co => s1
                        .group(kind = ContentKind.Symbol){ gbdr => gbdr
                            .text("Companion ")
                            .driLink(
                                ext.kind match {
                                    case dotty.dokka.Kind.Object => "class"
                                    case _ => "object"
                                },
                                co
                            )
                        }
                    }.contentForDescription(c)

                }
            }
            .group(styles = Set(ContentStyle.TabbedContent)) { b => b
                .contentForScope(c)
                .contentForConstructors(c)
                .contentForEnum(c)
            }
        contentBuilder.contentForDocumentable(c, buildBlock = buildBlock)
    }

    override def contentForMember(d: Documentable) = {
        def buildBlock = (builder: ScalaPageContentBuilder#ScalaDocumentableContentBuilder) => builder
            .group(kind = ContentKind.Cover){ bd => bd.cover(d.getName)() }
            .divergentGroup(
                ContentDivergentGroup.GroupID("member")
            ) { divbdr => divbdr
                .instance(Set(d.getDri), sourceSets = d.getSourceSets.asScala.toSet) { insbdr => insbdr
                    .before(){ bbdr => bbdr
                        .contentForDescription(d)
                        .contentForComments(d)
                    }
                    .divergent(kind = ContentKind.Symbol) { dbdr => dbdr
                        .signature(d)
                    }
                }
            }
        contentBuilder.contentForDocumentable(d, buildBlock = buildBlock)
    }

    override def contentForFunction(f: DFunction) = contentForMember(f)

    extension (b: ScalaPageContentBuilder#ScalaDocumentableContentBuilder):
        def descriptionIfNotEmpty(d: Documentable): ScalaPageContentBuilder#ScalaDocumentableContentBuilder = {
            val desc = contentForDescription(d).asScala.toSeq
            val res = if desc.isEmpty then b else b
                .sourceSetDependentHint(
                    Set(d.getDri), 
                    d.getSourceSets.asScala.toSet, 
                    kind = ContentKind.SourceSetDependentHint, 
                    styles = Set(TextStyle.UnderCoverText)
                ) { sourceSetBuilder => sourceSetBuilder
                        .addChildren(desc)
                }
            res
        }

        def contentForComments(d: Documentable) = b

        def contentForDescription(d: Documentable) = {
            val specialTags = Set[Class[_]](classOf[Description])
            val tags = d.getDocumentation.asScala.toList.flatMap( (pd, doc) => doc.getChildren.asScala.map(pd -> _).toList )

            val platforms = d.getSourceSets.asScala.toSet

            val description = tags.collect{ case (pd, d: Description) => (pd, d) }.drop(1).groupBy(_(0)).map( (key, value) => key -> value.map(_(1)))

            val unnamedTags = tags.filterNot( t => t(1).isInstanceOf[NamedTagWrapper] || specialTags.contains(t(1).getClass))
                .groupBy(t => (t(0), t(1).getClass))
                .map( (key, value) => key -> value.map(_(1)) )

            val namedTags = tags.collect{ case (sourcesets, n: NamedTagWrapper) => (sourcesets, n) }.groupBy(_._2.getName).map( (a,b) => (a,b.toMap))

            b.group(Set(d.getDri), styles = Set(TextStyle.Block, TableStyle.Borderless)) { bdr => 
                val b1 = description.foldLeft(bdr){ 
                    case (bdr, (key, value)) => bdr
                        .group(sourceSets = Set(key)){ gbdr => 
                            value.foldLeft(gbdr) { (gbdr, tag) => gbdr
                                .comment(tag.getRoot)
                            }
                        }
                }
                b1.table(kind = ContentKind.Comment, styles = Set(TableStyle.DescriptionList)){ tbdr =>
                    val withUnnamedTags = unnamedTags.foldLeft(tbdr){ case (bdr, (key, value) ) => bdr
                            .cell(sourceSets = Set(key(0))){ b => b
                                .text(key(1).getSimpleName, styles = Set(TextStyle.Bold))
                            }
                            .cell(sourceSets = Set(key(0))) { b => b
                                .list(value){ (bdr, elem) => bdr
                                    .comment(elem.getRoot)
                                }
                            }
                    }
                    val withNamedTags = namedTags.foldLeft(withUnnamedTags){ case (bdr, (key, value)) => value.foldLeft(bdr){ case (bdr, (sourceSets, v)) => bdr
                            .cell(sourceSets = Set(sourceSets)){ b => b
                                .text(key)
                            }
                            .cell(sourceSets = Set(sourceSets)){ b => b
                                .comment(v.getRoot)
                            }
                        }
                    }
                    d match{
                        case d: (WithExpectActual & WithExtraProperties[Documentable]) if d.get(SourceLinks) != null && !d.get(SourceLinks).links.isEmpty => d.get(SourceLinks).links.foldLeft(withNamedTags){
                            case (bdr, (sourceSet, link)) => bdr
                                .cell(sourceSets = Set(sourceSet)){ b => b
                                    .text("Source")
                                }
                                .cell(sourceSets = Set(sourceSet)){ b => b
                                    .resolvedLink("(source)", link)
                                }
                        }
                        case other => withNamedTags       
                    }

                }

            }
        }

        private def implicitConversionLink(dri: DRI)  = {
            val ifEmptyDri = if getParent(dri).getClassNames != null && getParent(dri).getClassNames.contains("$package$") 
            then getParent(getParent(dri))
            else getParent(dri)
            val ifEmptyName = if ifEmptyDri.getClassNames == null then ifEmptyDri.getPackageName else ifEmptyDri.getClassNames
            Option(dri.getCallable).fold(
                b.text("anonymous conversion in ").driLink(ifEmptyName, ifEmptyDri)
            )(
                c => b.driLink(c.getName, dri)
            )
        }

        def buildImplicitConversionInfo(by: DRI) = 
            b.table(kind = ContentKind.BriefComment, styles = Set(TableStyle.DescriptionList)){ tbdr => tbdr
                .cell() { c => c
                    .text("Implicitly: ")
                }
                .cell() { c => c
                    .text("This member is added by an implicit conversion performed by ")
                    .implicitConversionLink(by)
                }
            }

        private def inheritedExtensionLink(dri: DRI)  = {
            val ifEmptyDri = if dri.getClassNames != null && dri.getClassNames.contains("$package$") 
            then getParent(dri)
            else dri
            val ifEmptyName = ifEmptyDri.getPackageName
            Option(dri.getClassNames).filterNot(_.contains("$package$")).fold(
                b.text("package ").driLink(ifEmptyName, ifEmptyDri)
            )(
                c => b.driLink(c, dri)
            )
        }

        def buildInheritedExtensionInfo(by: DRI) = 
            b.table(kind = ContentKind.BriefComment, styles = Set(TableStyle.DescriptionList)){ tbdr => tbdr
                .cell() { c => c
                    .text("Implicitly: ")
                }
                .cell() { c => c
                    .text("This member is added by an extension defined in ")
                    .inheritedExtensionLink(by)
                }
            }

        def contentForScope(
            s: Documentable & WithScope
        ) = {
            val (typeDefs, valDefs) = s.getProperties.asScala.toList.partition(_.get(PropertyExtension).kind == "type")
            val classes = s.getClasslikes.asScala.toList
            val givens = s match {
                case p: DPackage => Option(p.get(PackageExtension)).map(_.givens).getOrElse(List.empty)
                case clazz: DClass => clazz.get(ClasslikeExtension).givens
                case other => List.empty
            }
            val extensions = s match {
                case p: DPackage => Option(p.get(PackageExtension)).map(_.extensions).getOrElse(List.empty)
                case clazz: DClass => clazz.get(ClasslikeExtension).extensions
                case other => List.empty
            }
            val implicits = s match {
                case c: DClass => c.get(ImplicitMembers)
                case other => ImplicitMembers()
            }
            val implicitMap = (
                implicits.classlikes ++ 
                implicits.properties ++ 
                implicits.methods ++ 
                implicits.inheritedMethods ++
                implicits.givens ++
                implicits.extensions.flatMap( (key, value) => key.extensions.map(_ -> value) ).toMap
            ).toMap
            val inherited = s match {
                case c: DClass => List("Inherited" -> (c.get(ClasslikeExtension).inheritedMethods ++ implicits.inheritedMethods.keys.toList))
                case other => List.empty
            }
            
            b.contentForComments(s)
            .groupingBlock(
                "Type members",
                List(
                    "Types" -> (typeDefs ++ implicits.properties.keys.toList.filter(_.get(PropertyExtension).kind == "type")), 
                    "Classlikes" -> (classes ++ implicits.classlikes.keys.toList)
                ),
                kind = ContentKind.Classlikes
            )(
                (bdr, elem) => bdr.header(3, elem)()
            ){ (bdr, elem) => bdr
                .driLink(elem.getName, elem.getDri)
                .sourceSetDependentHint(Set(elem.getDri), elem.getSourceSets.asScala.toSet, kind = ContentKind.SourceSetDependentHint) { sbdr => 
                    val withBrief = sbdr.contentForBrief(elem)
                    (if implicitMap.contains(elem) then withBrief.buildImplicitConversionInfo(implicitMap(elem)) else withBrief)
                    .signature(elem)
                }
            }
            .groupingBlock(
                "Methods",
                List(
                    "Class methods" -> (s.getFunctions.asScala.toList ++ implicits.methods.keys.toList ++ implicits.inheritedExtensions.keys.toList), 
                ) ++ inherited,
                kind = ContentKind.Functions,
                omitSplitterOnSingletons = false
            )(
                (builder, txt) => builder.header(3, txt)()
            ){ (bdr, elem) => bdr
                .driLink(elem.getName, elem.getDri)
                .sourceSetDependentHint(Set(elem.getDri), elem.getSourceSets.asScala.toSet, kind = ContentKind.SourceSetDependentHint) { sbdr => 
                    val withBrief = sbdr.contentForBrief(elem)
                    (if implicitMap.contains(elem) then withBrief.buildImplicitConversionInfo(implicitMap(elem)) 
                    else if implicits.inheritedExtensions.contains(elem) then withBrief.buildInheritedExtensionInfo(implicits.inheritedExtensions(elem))
                    else withBrief)
                    .signature(elem)
                }
            }
            .groupingBlock(
                "Value members",
                List("" -> (valDefs ++ implicits.properties.keys.toList.filter(_.get(PropertyExtension).kind != "type"))),
                kind = ContentKind.Properties,
                sourceSets = s.getSourceSets.asScala.toSet
            )(
                (bdr, group) => bdr
            ){ (bdr, elem) => bdr
                .driLink(elem.getName, elem.getDri)
                .sourceSetDependentHint(Set(elem.getDri), elem.getSourceSets.asScala.toSet, kind = ContentKind.SourceSetDependentHint) { sbdr => 
                    val withBrief = sbdr.contentForBrief(elem)
                    (if implicitMap.contains(elem) then withBrief.buildImplicitConversionInfo(implicitMap(elem)) else withBrief)
                    .signature(elem)
                }
            }
            .groupingBlock(
                "Given",
                if(!givens.isEmpty) List(() -> (givens.sortBy(_.getName).toList ++ implicits.givens.keys.toList)) else List.empty,
                kind = ContentKind.Functions
            )( (bdr, splitter) => bdr ){ (bdr, elem) => bdr
                .driLink(elem.getName, elem.getDri)
                .sourceSetDependentHint(Set(elem.getDri), elem.getSourceSets.asScala.toSet, kind = ContentKind.SourceSetDependentHint) { sbdr => 
                    val withBrief = sbdr.contentForBrief(elem)
                    (if implicitMap.contains(elem) then withBrief.buildImplicitConversionInfo(implicitMap(elem)) else withBrief)
                    .signature(elem)
                }
            }
            .groupingBlock(
                "Extensions",
                (implicits.extensions.keys.toList ++ extensions).map(e => e.extendedSymbol -> e.extensions).sortBy(_._2.size),
                kind = ContentKind.Extensions
            )( (bdr, receiver) => bdr 
                .group(){ grpbdr => grpbdr
                    .signature(receiver)
                }
            ){ (bdr, elem) => bdr
                    .driLink(elem.getName, elem.getDri)
                    .sourceSetDependentHint(Set(elem.getDri), elem.getSourceSets.asScala.toSet, kind = ContentKind.SourceSetDependentHint) { sbdr => 
                        val withBrief = sbdr.contentForBrief(elem)
                        (if implicitMap.contains(elem) then withBrief.buildImplicitConversionInfo(implicitMap(elem)) else withBrief)
                        .signature(elem)
                    }
            }
        }

        def contentForEnum(
            c: DClass
        ) = b.groupingBlock(
            "Enum entries",
            List(() -> Option(c.get(EnumExtension)).fold(List.empty)(_.enumEntries.sortBy(_.getName).toList)),
            kind = ContentKind.Properties
        )( (bdr, splitter) => bdr ){ (bdr, elem) => bdr
                .driLink(elem.getName, elem.getDri)
                .sourceSetDependentHint(Set(elem.getDri), elem.getSourceSets.asScala.toSet, kind = ContentKind.SourceSetDependentHint) { sbdr => sbdr
                    .contentForBrief(elem)
                    .signature(elem)
                }
        }

        def contentForConstructors(
                c: DClass
            ) = b.groupingBlock(
                "Constructors",
                List("" -> c.getConstructors.asScala.toList),
                kind = ContentKind.Constructors
            )(
                (bdr, group) => bdr
            ){ (bdr, elem) => bdr
                    .driLink(elem.getName, elem.getDri)
                    .sourceSetDependentHint(Set(elem.getDri), elem.getSourceSets.asScala.toSet, kind = ContentKind.SourceSetDependentHint) { sbdr => sbdr
                        .contentForBrief(elem)
                        .signature(elem)
                    }
            }
    

        def contentForTypesInfo(c: DClass) = {
            val inheritanceInfo = c.get(InheritanceInfo)
            val supertypes = inheritanceInfo.parents
            val subtypes = inheritanceInfo.knownChildren
            def contentForType(
                bdr: ScalaPageContentBuilder#ScalaDocumentableContentBuilder,
                b: DRI
            ): ScalaPageContentBuilder#ScalaDocumentableContentBuilder = bdr
                .driLink(b.getClassNames, b)

            def contentForBound(
                bdr: ScalaPageContentBuilder#ScalaDocumentableContentBuilder,
                b: Bound
            ): ScalaPageContentBuilder#ScalaDocumentableContentBuilder = b match {
                    case t: org.jetbrains.dokka.model.TypeConstructor => t.getProjections.asScala.foldLeft(bdr){
                        case (builder, p) => p match {
                            case text: UnresolvedBound => builder.text(text.getName)
                            case link: TypeParameter => builder.driLink(link.getName, link.getDri) 
                            case other => builder.text(s"TODO: $other")
                        }
                    }
                    case o => bdr.text(s"TODO: $o")
            }
            val withSupertypes = if(!supertypes.isEmpty) {
                b.header(2, "Linear supertypes")()
                .group(
                    kind = ContentKind.Comment,
                    styles = Set(ContentStyle.WithExtraAttributes), 
                    extra = PropertyContainer.Companion.empty plus SimpleAttr.Companion.header("Linear supertypes")
                ){ gbdr => gbdr
                    .group(kind = ContentKind.Symbol, styles = Set(TextStyle.Monospace)){ grbdr => grbdr
                        .list(supertypes, separator = ""){ (bdr, elem) => bdr
                            .group(styles = Set(TextStyle.Paragraph))(contentForBound(_, elem))
                        }
                    }
                }
            } else b

            if(!subtypes.isEmpty) {
                withSupertypes.header(2, "Known subtypes")()
                .group(
                    kind = ContentKind.Comment,
                    styles = Set(ContentStyle.WithExtraAttributes), 
                    extra = PropertyContainer.Companion.empty plus SimpleAttr.Companion.header("Known subtypes")
                ){ gbdr => gbdr
                    .group(kind = ContentKind.Symbol, styles = Set(TextStyle.Monospace)){ grbdr => grbdr
                        .list(subtypes)(contentForType)
                    }
                }
            } else withSupertypes

        }

}