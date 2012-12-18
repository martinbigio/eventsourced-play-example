package views.html.helper

package object bootstrap {
  implicit val twitterBootstrapField = new FieldConstructor {
    def apply(elts: FieldElements) = bootstrapFieldConstructor(elts)
  }
}