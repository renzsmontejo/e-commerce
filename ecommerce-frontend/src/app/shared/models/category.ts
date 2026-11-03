export interface Category {
  id: number;
  name: string;
  slug: string;
  parentId?: number | null;
  description?: string | null;
  /** Path under /public, e.g. /images/categories/home-living.png */
  imageUrl?: string;
  /** Tile background colour (home page category grid) */
  tint?: string;
}
